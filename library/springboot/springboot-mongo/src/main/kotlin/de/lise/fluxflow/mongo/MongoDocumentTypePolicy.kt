package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeRole
import org.bson.Document
import org.bson.conversions.Bson
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap

/** Validates every persisted type discriminator before Spring Data may resolve it. */
internal class MongoDocumentTypePolicy(
    private val aliases: FluxFlowMongoTypeAliases,
    private val typeKey: String = "_class",
    private val maxDepth: Int = 100,
    private val maxNodes: Int = 100_000,
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

        val pending = ArrayDeque<Frame>()
        root.entries.asSequence()
            .filterNot { (key, _) -> key == typeKey }
            .forEach { (key, value) ->
                pending.add(Frame(value, key, 1, policyForRootField(targetType, key)))
            }

        validateFrames(pending)
    }

    fun validateProjection(targetType: Class<*>, source: Bson) {
        val root = source as? Document
            ?: throw IllegalArgumentException("Mongo data must be represented by a BSON Document")
        val rootPolicy = when (aliases.rolesOf(targetType)) {
            setOf(TypeRole.MODEL) -> NodePolicy.Model
            else -> NodePolicy.Application(TypeRole.VALUE)
        }
        validateAlias(root, rootPolicy, "projection")
        val pending = ArrayDeque<Frame>()
        root.entries.asSequence()
            .filterNot { (key, _) -> key == typeKey }
            .forEach { (key, value) ->
                pending.add(Frame(value, key, 1, rootPolicy.forField(key)))
            }
        validateFrames(pending)
    }

    /**
     * Inspects discriminator aliases without asking Spring Data or a class loader to resolve them.
     * The traversal deliberately shares the same field policies and limits as [validate].
     */
    fun auditAliases(
        targetType: Class<*>,
        source: Bson,
        isRegisteredValueType: (Any?) -> Boolean,
    ): List<MongoTypeAliasDeviation> {
        val root = source as? Document
            ?: throw IllegalArgumentException("Mongo data must be represented by a BSON Document")
        val deviations = mutableListOf<MongoTypeAliasDeviation>()

        if (root.containsKey(typeKey) && !aliases.isAliasFor(root[typeKey], targetType)) {
            deviations += deviation("/${pointerSegment(typeKey)}", null, root[typeKey], isRegisteredValueType)
        }

        val pending = ArrayDeque<AuditFrame>()
        root.entries.asSequence()
            .filterNot { (key, _) -> key == typeKey }
            .forEach { (key, value) ->
                pending.add(
                    AuditFrame(
                        value,
                        "/${pointerSegment(key)}",
                        1,
                        policyForRootField(targetType, key),
                    )
                )
            }

        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        var nodes = 1
        while (pending.isNotEmpty()) {
            val frame = pending.removeLast()
            nodes++
            require(nodes <= maxNodes) {
                "Mongo document exceeds the maximum node count of $maxNodes"
            }
            require(frame.depth <= maxDepth) {
                "Mongo document exceeds the maximum depth of $maxDepth at '${frame.path}'"
            }

            when (val value = frame.value) {
                is Map<*, *> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    if (value.containsKey(typeKey) && !isAllowed(value[typeKey], frame.policy)) {
                        deviations += deviation(
                            "${frame.path}/${pointerSegment(typeKey)}",
                            expectedRole(frame.policy),
                            value[typeKey],
                            isRegisteredValueType,
                        )
                    }
                    value.entries.asSequence()
                        .filterNot { (key, _) -> key == typeKey }
                        .forEach { (key, nested) ->
                            pending.add(
                                AuditFrame(
                                    nested,
                                    "${frame.path}/${pointerSegment(key?.toString().orEmpty())}",
                                    frame.depth + 1,
                                    frame.policy.forField(key?.toString()),
                                )
                            )
                        }
                }

                is Iterable<*> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    value.forEachIndexed { index, nested ->
                        require(nodes + pending.size < maxNodes) {
                            "Mongo document exceeds the maximum node count of $maxNodes"
                        }
                        pending.add(
                            AuditFrame(
                                nested,
                                "${frame.path}/$index",
                                frame.depth + 1,
                                frame.policy.forElement(),
                            )
                        )
                    }
                }

                is Array<*> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    value.forEachIndexed { index, nested ->
                        pending.add(
                            AuditFrame(
                                nested,
                                "${frame.path}/$index",
                                frame.depth + 1,
                                frame.policy.forElement(),
                            )
                        )
                    }
                }
            }
        }
        return deviations
    }

    private fun validateFrames(pending: ArrayDeque<Frame>) {
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        var nodes = 1
        while (pending.isNotEmpty()) {
            val frame = pending.removeLast()
            nodes++
            require(nodes <= maxNodes) {
                "Mongo document exceeds the maximum node count of $maxNodes"
            }
            require(frame.depth <= maxDepth) {
                "Mongo document exceeds the maximum depth of $maxDepth at '${frame.path}'"
            }

            when (val value = frame.value) {
                is Map<*, *> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    validateAlias(value, frame.policy, frame.path)
                    value.entries.asSequence()
                        .filterNot { (key, _) -> key == typeKey }
                        .forEach { (key, nested) ->
                            pending.add(
                                Frame(
                                    nested,
                                    "${frame.path}.$key",
                                    frame.depth + 1,
                                    frame.policy.forField(key?.toString()),
                                )
                            )
                        }
                }

                is Iterable<*> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    var index = 0
                    for (nested in value) {
                        require(nodes + pending.size < maxNodes) {
                            "Mongo document exceeds the maximum node count of $maxNodes"
                        }
                        pending.add(
                            Frame(
                                nested,
                                "${frame.path}[$index]",
                                frame.depth + 1,
                                frame.policy.forElement(),
                            )
                        )
                        index++
                    }
                }

                is Array<*> -> {
                    if (!visited.add(value)) {
                        continue
                    }
                    value.forEachIndexed { index, nested ->
                        pending.add(
                            Frame(
                                nested,
                                "${frame.path}[$index]",
                                frame.depth + 1,
                                frame.policy.forElement(),
                            )
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun policyForRootField(targetType: Class<*>, key: String): NodePolicy = when (targetType) {
        WorkflowDocument::class.java -> when (key) {
            WorkflowDocument::model.name -> NodePolicy.Model
            else -> NodePolicy.Static
        }

        StepDocument::class.java -> when (key) {
            StepDocument::data.name,
            StepDocument::metadata.name -> NodePolicy.Application(TypeRole.VALUE)
            StepDocument::dataTypeMap.name,
            StepDocument::metadataTypeMap.name -> NodePolicy.Infrastructure
            StepDocument::dataEntries.name,
            StepDocument::metadataEntries.name -> NodePolicy.TypedRecords
            else -> NodePolicy.Static
        }

        JobDocument::class.java -> when (key) {
            JobDocument::parameters.name -> NodePolicy.Application(TypeRole.VALUE)
            JobDocument::parameterTypeMap.name -> NodePolicy.Infrastructure
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

    private fun validateAlias(document: Map<*, *>, policy: NodePolicy, path: String) {
        if (!document.containsKey(typeKey)) {
            return
        }
        val alias = document[typeKey]
        try {
            when (policy) {
                is NodePolicy.Application -> aliases.resolve(policy.role, alias)
                NodePolicy.Model -> aliases.resolve(TypeRole.MODEL, alias)
                NodePolicy.Infrastructure -> require(aliases.isInfrastructure(alias)) {
                    "Mongo type alias '$alias' is not allowed in FluxFlow infrastructure metadata"
                }
                NodePolicy.TypedRecords -> require(aliases.isAliasFor(alias, TypedRecords::class.java)) {
                    "Mongo type alias '$alias' is not a TypedRecords container"
                }
                NodePolicy.StepDefinitionData -> require(
                    aliases.isAliasFor(alias, DataDefinitionDocument::class.java)
                ) {
                    "Mongo type alias '$alias' is not a step data definition"
                }
                NodePolicy.Static -> throw IllegalArgumentException(
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
        NodePolicy.Infrastructure -> aliases.isInfrastructure(alias)
        NodePolicy.TypedRecords -> aliases.isAliasFor(alias, TypedRecords::class.java)
        NodePolicy.StepDefinitionData -> aliases.isAliasFor(alias, DataDefinitionDocument::class.java)
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

    private fun pointerSegment(value: String): String = value
        .replace("~", "~0")
        .replace("/", "~1")

    private data class Frame(
        val value: Any?,
        val path: String,
        val depth: Int,
        val policy: NodePolicy,
    )

    private data class AuditFrame(
        val value: Any?,
        val path: String,
        val depth: Int,
        val policy: NodePolicy,
    )

    private sealed interface NodePolicy {
        fun forField(key: String?): NodePolicy
        fun forElement(): NodePolicy

        data class Application(val role: TypeRole) : NodePolicy {
            override fun forField(key: String?): NodePolicy = this
            override fun forElement(): NodePolicy = this
        }

        data object Model : NodePolicy {
            override fun forField(key: String?): NodePolicy = Application(TypeRole.VALUE)
            override fun forElement(): NodePolicy = Application(TypeRole.VALUE)
        }

        data object Infrastructure : NodePolicy {
            override fun forField(key: String?): NodePolicy = this
            override fun forElement(): NodePolicy = this
        }

        data object TypedRecords : NodePolicy {
            override fun forField(key: String?): NodePolicy = when (key) {
                "values" -> Application(TypeRole.VALUE)
                "jvmTypes", "types" -> Infrastructure
                else -> Static
            }

            override fun forElement(): NodePolicy = Static
        }

        data object StepDefinitionData : NodePolicy {
            override fun forField(key: String?): NodePolicy = when (key) {
                StepDefinitionDocument::data.name -> this
                StepDefinitionDocument::metadata.name -> TypedRecords
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
