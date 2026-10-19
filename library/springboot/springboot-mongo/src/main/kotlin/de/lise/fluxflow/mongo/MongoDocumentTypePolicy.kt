package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import org.bson.Document
import org.bson.conversions.Bson
import java.util.ArrayDeque
import java.util.IdentityHashMap

/** Validates every persisted type discriminator before Spring Data may resolve it. */
internal class MongoDocumentTypePolicy(
    private val aliases: FluxFlowMongoTypeAliases,
    private val typeKey: String = "_class",
    private val maxDepth: Int = 100,
    private val maxNodes: Int = 100_000,
    private val fieldNames: MongoFieldNames? = null,
    private val isRegisteredJvmValueType: (Any?) -> Boolean,
) {
    init {
        require(maxDepth > 0) { "Mongo document maximum depth must be positive" }
        require(maxNodes > 0) { "Mongo document maximum node count must be positive" }
    }

    fun validate(targetType: Class<*>, source: Bson) {
        if (targetType !in aliases.rootTypes) {
            validateProjection(targetType, source)
            return
        }
        val root = source as? Document
            ?: throw IllegalArgumentException("Mongo data must be represented by a BSON Document")

        validateRootAlias(targetType, root)

        walk(
            root.entries.asSequence()
                .filterNot { (key, _) -> key == typeKey }
                .map { (key, value) ->
                    PendingFrame(
                        value,
                        1,
                        path = { DocumentPath(field = key) },
                        policy = { policyForRootField(targetType, key) },
                    )
                },
            onMap = { value, policy, path ->
                validateAlias(value, policy, path)
                validateJvmTypeName(value, policy)
            },
            onLimit = ::readLimit,
            pathBeforeNodeBudget = false,
        )
    }

    fun validateProjection(targetType: Class<*>, source: Bson) {
        val root = source as? Document
            ?: throw IllegalArgumentException("Mongo data must be represented by a BSON Document")
        val roles = aliases.rolesOf(targetType)
        val rootPolicy = when {
            targetType == TypedRecords::class.java -> NodePolicy.TypedRecords
            targetType == JvmTypeMapping::class.java -> NodePolicy.JvmTypes
            targetType == TypeRecordEntry::class.java -> NodePolicy.JvmTypeEntry
            targetType == DataDefinitionDocument::class.java -> NodePolicy.StepDefinitionData
            targetType in aliases.infrastructureTypes -> NodePolicy.Infrastructure
            roles == setOf(TypeRole.MODEL) -> NodePolicy.Model
            roles == setOf(TypeRole.MODEL, TypeRole.VALUE) -> NodePolicy.Projection(roles)
            else -> NodePolicy.Application(TypeRole.VALUE)
        }
        validateAlias(root, rootPolicy, DocumentPath(field = "projection"))
        validateJvmTypeName(root, rootPolicy)
        walk(
            root.entries.asSequence()
                .filterNot { (key, _) -> key == typeKey }
                .map { (key, value) ->
                    PendingFrame(
                        value,
                        1,
                        path = { DocumentPath(field = key) },
                        policy = { policyForField(rootPolicy, key) },
                    )
                },
            onMap = { value, policy, path ->
                validateAlias(value, policy, path)
                validateJvmTypeName(value, policy)
            },
            onLimit = ::readLimit,
            pathBeforeNodeBudget = false,
        )
    }

    /**
     * Inspects discriminator aliases without asking Spring Data or a class loader to resolve them.
     * The traversal deliberately shares the same field policies and limits as [validate].
     * @throws MongoTypeAuditInspectionException if a traversal limit prevents inspecting a document location.
     */
    fun auditAliases(
        targetType: Class<*>,
        source: Bson,
        isRegisteredValueType: (Any?) -> Boolean,
        isKnownInAnotherRoleValueType: (Any?) -> Boolean = { aliases.isRegistered(it) },
    ): List<MongoTypeAliasDeviation> {
        val root = source as? Document
            ?: throw IllegalArgumentException("Mongo data must be represented by a BSON Document")
        val deviations = mutableListOf<MongoTypeAliasDeviation>()

        if (root.containsKey(typeKey) && !aliases.isAliasFor(root[typeKey], targetType)) {
            deviations += deviation("/${pointerSegment(typeKey)}", null, root[typeKey], isRegisteredValueType)
        }

        walk(
            root.entries.asSequence()
                .filterNot { (key, _) -> key == typeKey }
                .map { (key, value) ->
                    PendingFrame(
                        value,
                        1,
                        path = { DocumentPath(field = key) },
                        policy = { policyForRootField(targetType, key) },
                    )
                },
            onMap = { value, policy, path ->
                if (value.containsKey(typeKey) && !isAllowed(value[typeKey], policy)) {
                    deviations += deviation(
                        path.childField(typeKey, typeKey).pointerString(),
                        expectedRole(policy),
                        value[typeKey],
                        isRegisteredValueType,
                    )
                }
            },
            onValueTypeName = { path, value ->
                if (!isRegisteredValueType(value)) {
                    val persistedName = value as? String
                    val issue = when {
                        persistedName.isNullOrEmpty() -> MongoTypeAliasIssue.MALFORMED
                        isKnownInAnotherRoleValueType(value) -> MongoTypeAliasIssue.DISALLOWED
                        else -> MongoTypeAliasIssue.UNREGISTERED
                    }
                    deviations += MongoTypeAliasDeviation(
                        path.pointerString(),
                        TypeRole.VALUE,
                        persistedName,
                        issue,
                    )
                }
            },
            onLimit = ::auditLimit,
            pathBeforeNodeBudget = true,
            auditTypeNameValues = true,
        )
        return deviations
    }

    private fun walk(
        seeds: Sequence<PendingFrame>,
        onMap: (Map<*, *>, NodePolicy, DocumentPath) -> Unit,
        onValueTypeName: (DocumentPath, Any?) -> Unit = { _, _ -> },
        onLimit: (DocumentPath, LimitKind) -> Nothing,
        pathBeforeNodeBudget: Boolean,
        auditTypeNameValues: Boolean = false,
    ) {
        val pending = ArrayDeque<Frame>()
        var nodes = 1
        fun enqueue(pendingFrame: PendingFrame) {
            val earlyPath = if (pathBeforeNodeBudget) pendingFrame.path() else null
            if (nodes + pending.size >= maxNodes) {
                onLimit(earlyPath ?: DocumentPath(), LimitKind.NODE)
            }
            val path = earlyPath ?: pendingFrame.path()
            pending.add(
                Frame(
                    pendingFrame.value,
                    path,
                    pendingFrame.depth,
                    pendingFrame.policy(),
                    pendingFrame.auditTypeNameScope,
                    pendingFrame.inspectAliases,
                )
            )
        }
        seeds.forEach(::enqueue)

        val visited = IdentityHashMap<Any, MutableSet<NodePolicy>>()
        while (pending.isNotEmpty()) {
            val frame = pending.removeLast()
            nodes++
            if (nodes > maxNodes) {
                onLimit(frame.path, LimitKind.NODE)
            }
            if (frame.depth > maxDepth) {
                onLimit(frame.path, LimitKind.DEPTH)
            }

            when (val value = frame.value) {
                is Map<*, *> -> {
                    val firstAliasVisit = frame.inspectAliases &&
                        visited.getOrPut(value) { mutableSetOf() }.add(frame.policy)
                    if (!firstAliasVisit && !frame.auditTypeNameScope) {
                        continue
                    }
                    if (firstAliasVisit) {
                        onMap(value, frame.policy, frame.path)
                    }
                    if (auditTypeNameValues && frame.auditTypeNameScope && frame.policy == NodePolicy.Infrastructure) {
                        val typeNamePresent = fieldNames?.contains(value, SimpleType::class.java, "typeName")
                            ?: value.containsKey("typeName")
                        if (typeNamePresent) {
                            val typeNameParts = fieldNames?.fieldParts(SimpleType::class.java, "typeName")
                                ?: listOf("typeName")
                            var typeNamePath = frame.path
                            typeNameParts.forEach { part ->
                                typeNamePath = typeNamePath.childField(part, part)
                            }
                            val typeName = fieldNames?.read(value, SimpleType::class.java, "typeName")
                                ?: if (fieldNames == null) value["typeName"] else null
                            onValueTypeName(typeNamePath, typeName)
                        }
                    }
                    value.entries.asSequence()
                        .filter { (key, nested) ->
                            val scansTypeMapValue = auditTypeNameValues && frame.policy == NodePolicy.TypeMap
                            val scansScopedValue = frame.auditTypeNameScope
                            val scanTypeNameSubtree = scansTypeMapValue || scansScopedValue
                            !(key == typeKey && !scanTypeNameSubtree) &&
                                !(key == typeKey && scanTypeNameSubtree && !isStructuredValue(nested))
                        }
                        .forEach { (key, nested) ->
                            val childTypeNameScope = when {
                                auditTypeNameValues && frame.policy == NodePolicy.TypeMap -> true
                                frame.auditTypeNameScope &&
                                    frame.policy == NodePolicy.Infrastructure &&
                                    (fieldNames?.fieldParts(SimpleType::class.java, "typeName")
                                        ?: listOf("typeName")).singleOrNull() == key -> false
                                else -> frame.auditTypeNameScope
                            }
                            enqueue(
                                PendingFrame(
                                    nested,
                                    frame.depth + 1,
                                    path = {
                                        val keyText = key?.toString()
                                        frame.path.childField(keyText ?: "null", keyText.orEmpty())
                                    },
                                    policy = { policyForField(frame.policy, key?.toString()) },
                                    auditTypeNameScope = childTypeNameScope,
                                    inspectAliases = frame.inspectAliases && key != typeKey,
                                )
                            )
                        }
                }

                is Iterable<*> -> {
                    val firstAliasVisit = frame.inspectAliases &&
                        visited.getOrPut(value) { mutableSetOf() }.add(frame.policy)
                    if (!firstAliasVisit && !frame.auditTypeNameScope) {
                        continue
                    }
                    var index = 0
                    for (nested in value) {
                        enqueue(
                            PendingFrame(
                                nested,
                                frame.depth + 1,
                                path = { frame.path.childIndex(index) },
                                policy = { frame.policy.forElement() },
                                auditTypeNameScope = frame.auditTypeNameScope,
                                inspectAliases = frame.inspectAliases,
                            )
                        )
                        index++
                    }
                }

                is Array<*> -> {
                    val firstAliasVisit = frame.inspectAliases &&
                        visited.getOrPut(value) { mutableSetOf() }.add(frame.policy)
                    if (!firstAliasVisit && !frame.auditTypeNameScope) {
                        continue
                    }
                    value.forEachIndexed { index, nested ->
                        enqueue(
                            PendingFrame(
                                nested,
                                frame.depth + 1,
                                path = { frame.path.childIndex(index) },
                                policy = { frame.policy.forElement() },
                                auditTypeNameScope = frame.auditTypeNameScope,
                                inspectAliases = frame.inspectAliases,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun readLimit(path: DocumentPath, kind: LimitKind): Nothing = when (kind) {
        LimitKind.NODE -> throw IllegalArgumentException(
            "Mongo document exceeds the maximum node count of $maxNodes"
        )
        LimitKind.DEPTH -> throw IllegalArgumentException(
            "Mongo document exceeds the maximum depth of $maxDepth at '$path'"
        )
    }

    private fun auditLimit(path: DocumentPath, kind: LimitKind): Nothing = when (kind) {
        LimitKind.NODE -> throw MongoTypeAuditInspectionException(
            path.pointerString(),
            "Mongo document exceeds the maximum node count of $maxNodes",
        )
        LimitKind.DEPTH -> throw MongoTypeAuditInspectionException(
            path.pointerString(),
            "Mongo document exceeds the maximum depth of $maxDepth at '${path.pointerString()}'",
        )
    }

    private enum class LimitKind {
        NODE,
        DEPTH,
    }

    private fun isStructuredValue(value: Any?): Boolean =
        value is Map<*, *> || value is Iterable<*> || value is Array<*>

    private fun policyForRootField(targetType: Class<*>, key: String): NodePolicy =
        policyForField(NodePolicy.RootFields(targetType), key)

    private fun validateJvmTypeName(document: Map<*, *>, policy: NodePolicy) {
        val type = when (policy) {
            NodePolicy.Infrastructure -> SimpleType::class.java
            NodePolicy.JvmTypeEntry -> TypeRecordEntry::class.java
            else -> return
        }
        val property = if (type == SimpleType::class.java) "typeName" else "type"
        val present = fieldNames?.contains(document, type, property) ?: document.containsKey(property)
        if (present) {
            val name = if (fieldNames != null) fieldNames.read(document, type, property) else document[property]
            require(name is String && name.isNotEmpty()) { "Mongo JVM type name must be a non-empty string" }
            if (!isRegisteredJvmValueType(name)) {
                throw UnknownTypeException(TypeRole.VALUE, name)
            }
        }
    }

    private fun policyForField(policy: NodePolicy, key: String?): NodePolicy {
        val base = if (policy is NodePolicy.Namespace) policy.base else policy
        val mappedType = when (base) {
            is NodePolicy.RootFields -> base.type
            NodePolicy.TypedRecords -> TypedRecords::class.java
            NodePolicy.JvmTypes -> JvmTypeMapping::class.java
            NodePolicy.JvmTypeEntry -> TypeRecordEntry::class.java
            NodePolicy.StepDefinitionData -> DataDefinitionDocument::class.java
            else -> null
        }
        if (mappedType == null || fieldNames == null || key == null) {
            return if (base is NodePolicy.RootFields) policyForRootProperty(base.type, key) else base.forField(key)
        }
        val path = (if (policy is NodePolicy.Namespace) policy.path else emptyList()) + key
        val property = fieldNames.propertyAt(mappedType, path)
        if (property != null) {
            return if (base is NodePolicy.RootFields) policyForRootProperty(base.type, property) else base.forField(property)
        }
        return if (fieldNames.isNamespace(mappedType, path)) NodePolicy.Namespace(mappedType, path, base)
            else NodePolicy.Static
    }

    @Suppress("DEPRECATION")
    private fun policyForRootProperty(targetType: Class<*>, key: String?): NodePolicy = when (targetType) {
        WorkflowDocument::class.java -> when (key) {
            WorkflowDocument::model.name -> NodePolicy.Model
            else -> NodePolicy.Static
        }

        StepDocument::class.java -> when (key) {
            StepDocument::data.name,
            StepDocument::metadata.name -> NodePolicy.Application(TypeRole.VALUE)
            StepDocument::dataTypeMap.name,
            StepDocument::metadataTypeMap.name -> NodePolicy.TypeMap
            StepDocument::dataEntries.name,
            StepDocument::metadataEntries.name -> NodePolicy.TypedRecords
            else -> NodePolicy.Static
        }

        JobDocument::class.java -> when (key) {
            JobDocument::parameters.name -> NodePolicy.Application(TypeRole.VALUE)
            JobDocument::parameterTypeMap.name -> NodePolicy.TypeMap
            JobDocument::parameterEntries.name -> NodePolicy.TypedRecords
            else -> NodePolicy.Static
        }

        StepDefinitionDocument::class.java -> when (key) {
            StepDefinitionDocument::metadata.name -> NodePolicy.TypedRecords
            StepDefinitionDocument::data.name -> NodePolicy.StepDefinitionData
            else -> NodePolicy.Static
        }

        else -> NodePolicy.Static
    }

    private fun validateRootAlias(targetType: Class<*>, root: Document) {
        if (!root.containsKey(typeKey)) {
            return
        }
        val resolved = aliases.resolve(root[typeKey])
        if (targetType in aliases.rootTypes) {
            require(resolved == targetType) {
                "Mongo root type '${resolved.name}' does not match requested type '${targetType.name}'"
            }
        }
    }

    private fun validateAlias(document: Map<*, *>, policy: NodePolicy, path: DocumentPath) {
        if (!document.containsKey(typeKey)) {
            return
        }
        val alias = document[typeKey]
        val aliasText = alias as? String ?: "<non-string>"
        try {
            when (policy) {
                is NodePolicy.Application -> aliases.resolve(policy.role, alias)
                is NodePolicy.Projection -> aliases.resolve(policy.roles, alias)
                NodePolicy.Model -> aliases.resolve(TypeRole.MODEL, alias)
                NodePolicy.Infrastructure, NodePolicy.JvmTypes, NodePolicy.JvmTypeEntry -> require(aliases.isInfrastructure(alias)) {
                    "Mongo type alias '$aliasText' is not allowed in FluxFlow infrastructure metadata"
                }
                NodePolicy.TypedRecords -> require(aliases.isAliasFor(alias, TypedRecords::class.java)) {
                    "Mongo type alias '$aliasText' is not a TypedRecords container"
                }
                NodePolicy.StepDefinitionData -> require(
                    aliases.isAliasFor(alias, DataDefinitionDocument::class.java)
                ) {
                    "Mongo type alias '$aliasText' is not a step data definition"
                }
                NodePolicy.Static, NodePolicy.TypeMap, is NodePolicy.Namespace, is NodePolicy.RootFields -> throw IllegalArgumentException(
                    "Mongo type metadata is not allowed at '$path.$typeKey'"
                )
            }
        } catch (exception: IllegalArgumentException) {
            exception.addSuppressed(IllegalArgumentException("Invalid Mongo type at '$path.$typeKey'"))
            throw exception
        }
    }

    private fun isAllowed(
        alias: Any?,
        policy: NodePolicy,
    ): Boolean = when (policy) {
        is NodePolicy.Application -> aliases.isRegistered(policy.role, alias)
        NodePolicy.Model -> aliases.isRegistered(TypeRole.MODEL, alias)
        NodePolicy.Infrastructure, NodePolicy.JvmTypes, NodePolicy.JvmTypeEntry -> aliases.isInfrastructure(alias)
        NodePolicy.TypedRecords -> aliases.isAliasFor(alias, TypedRecords::class.java)
        NodePolicy.StepDefinitionData -> aliases.isAliasFor(alias, DataDefinitionDocument::class.java)
        is NodePolicy.Projection -> policy.roles.any { aliases.isRegistered(it, alias) }
        is NodePolicy.Namespace,
        is NodePolicy.RootFields,
        NodePolicy.TypeMap,
        NodePolicy.Static -> false
    }

    private fun expectedRole(policy: NodePolicy): TypeRole? = when (policy) {
        is NodePolicy.Application -> policy.role
        NodePolicy.Model -> TypeRole.MODEL
        else -> null
    }

    private fun deviation(
        path: String,
        expectedRole: TypeRole?,
        alias: Any?,
        isRegisteredValueType: (Any?) -> Boolean,
    ): MongoTypeAliasDeviation {
        val persistedName = alias as? String
        val issue = when {
            persistedName.isNullOrEmpty() -> MongoTypeAliasIssue.MALFORMED
            aliases.isRegistered(alias) || isRegisteredValueType(alias) -> MongoTypeAliasIssue.DISALLOWED
            else -> MongoTypeAliasIssue.UNREGISTERED
        }
        return MongoTypeAliasDeviation(path, expectedRole, persistedName, issue)
    }

    private data class Frame(
        val value: Any?,
        val path: DocumentPath,
        val depth: Int,
        val policy: NodePolicy,
        val auditTypeNameScope: Boolean,
        val inspectAliases: Boolean,
    )

    private data class PendingFrame(
        val value: Any?,
        val depth: Int,
        val path: () -> DocumentPath,
        val policy: () -> NodePolicy,
        val auditTypeNameScope: Boolean = false,
        val inspectAliases: Boolean = true,
    )

    /** Successful traversal keeps only parent links; diagnostic strings are built on failure. */
    private class DocumentPath(
        val parent: DocumentPath? = null,
        val field: String? = null,
        private val pointerField: String? = field,
        val index: Int = -1,
    ) {
        fun childField(readField: String, auditField: String): DocumentPath =
            DocumentPath(this, readField, auditField)

        fun childIndex(index: Int): DocumentPath = DocumentPath(this, index = index)

        override fun toString(): String {
            val segments = ArrayDeque<DocumentPath>()
            var current: DocumentPath? = this
            while (current != null) {
                segments.addFirst(current)
                current = current.parent
            }
            return buildString {
                var first = true
                for (segment in segments) {
                    if (segment.field != null) {
                        if (!first) append('.')
                        append(segment.field)
                    } else {
                        append('[').append(segment.index).append(']')
                    }
                    first = false
                }
            }
        }

        fun pointerString(): String {
            val segments = ArrayDeque<DocumentPath>()
            var current: DocumentPath? = this
            while (current != null) {
                segments.addFirst(current)
                current = current.parent
            }
            return buildString {
                for (segment in segments) {
                    if (segment.field != null) {
                        append('/')
                        append(pointerSegment(segment.pointerField ?: segment.field))
                    } else if (segment.index >= 0) {
                        append('/').append(segment.index)
                    }
                }
            }
        }
    }

    private sealed interface NodePolicy {
        fun forField(key: String?): NodePolicy
        fun forElement(): NodePolicy

        data class RootFields(val type: Class<*>) : NodePolicy {
            override fun forField(key: String?): NodePolicy = Static
            override fun forElement(): NodePolicy = Static
        }

        /** Mapping path containers carry no discriminator authorization of their own. */
        data class Namespace(val type: Class<*>, val path: List<String>, val base: NodePolicy) : NodePolicy {
            override fun forField(key: String?): NodePolicy = Static
            override fun forElement(): NodePolicy = Static
        }

        data class Application(val role: TypeRole) : NodePolicy {
            override fun forField(key: String?): NodePolicy = this
            override fun forElement(): NodePolicy = this
        }

        data object Model : NodePolicy {
            override fun forField(key: String?): NodePolicy = Application(TypeRole.VALUE)
            override fun forElement(): NodePolicy = Application(TypeRole.VALUE)
        }

        /** An isolated projection may originate in either registered application context. */
        data class Projection(val roles: Set<TypeRole>) : NodePolicy {
            override fun forField(key: String?): NodePolicy = Application(TypeRole.VALUE)
            override fun forElement(): NodePolicy = Application(TypeRole.VALUE)
        }

        data object Infrastructure : NodePolicy {
            override fun forField(key: String?): NodePolicy = this
            override fun forElement(): NodePolicy = this
        }

        /** The keys here belong to the application, not to SimpleType's schema. */
        data object TypeMap : NodePolicy {
            override fun forField(key: String?): NodePolicy = Infrastructure
            override fun forElement(): NodePolicy = Static
        }

        data object TypedRecords : NodePolicy {
            override fun forField(key: String?): NodePolicy = when (key) {
                "values" -> Application(TypeRole.VALUE)
                "jvmTypes" -> JvmTypes
                "types" -> TypeMap
                else -> Static
            }

            override fun forElement(): NodePolicy = Static
        }

        data object JvmTypes : NodePolicy {
            override fun forField(key: String?): NodePolicy = if (key == "entries") JvmTypeEntry else Static
            override fun forElement(): NodePolicy = Static
        }

        data object JvmTypeEntry : NodePolicy {
            override fun forField(key: String?): NodePolicy = Static
            override fun forElement(): NodePolicy = this
        }

        data object StepDefinitionData : NodePolicy {
            override fun forField(key: String?): NodePolicy = when (key) {
                DataDefinitionDocument::metadata.name -> TypedRecords
                else -> Static
            }

            override fun forElement(): NodePolicy = this
        }

        data object Static : NodePolicy {
            override fun forField(key: String?): NodePolicy = this
            override fun forElement(): NodePolicy = this
        }
    }
}

internal data class MongoTypeAliasDeviation(
    val path: String,
    val expectedRole: TypeRole?,
    val persistedName: String?,
    val issue: MongoTypeAliasIssue,
)

internal enum class MongoTypeAliasIssue {
    UNREGISTERED,
    MALFORMED,
    DISALLOWED,
}

private fun pointerSegment(value: String): String = value
    .replace("~", "~0")
    .replace("/", "~1")
