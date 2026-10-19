package de.lise.fluxflow.mongo.audit

import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoCollection
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.MongoTypeAliasIssue
import de.lise.fluxflow.mongo.MongoTypeAuditInspectionException
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.record.CollectionTypeRecord
import de.lise.fluxflow.mongo.generic.record.JvmTypeRecord
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypeReference
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeRole
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistries
import org.bson.types.ObjectId
import java.util.ArrayDeque
import java.util.IdentityHashMap
import java.util.UUID

/**
 * Uses the synchronous driver directly so the scan never invokes the mapping converter.
 * Field values and diagnostic paths use the same host mapping metadata as the read guard.
 */
internal class RawBsonFluxFlowMongoTypeAudit(
    private val access: FluxFlowMongoAccess,
    private val collectionProvider: (String) -> MongoCollection<Document> = { collectionName ->
        access.template.getCollection(collectionName)
    },
) : FluxFlowMongoTypeAudit {
    private val roleNames = access.typeRegistry.entries
        .groupBy { it.role }
        .mapValues { (_, entries) -> entries.mapTo(mutableSetOf()) { it.key } }
    private val modelTypesByName = access.typeRegistry.entries
        .filter { it.role == TypeRole.MODEL }
        .flatMap { entry ->
            buildSet {
                add(entry.key)
                add(entry.binaryClassName)
                entry.type.qualifiedName?.let(::add)
            }.map { name -> name to entry.type.java }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, types) -> types.toSet() }

    override fun audit(options: MongoTypeAuditOptions): MongoTypeAuditReport {
        val targets = targets().sortedBy { it.collection }
        val counts = linkedMapOf<String, Long>()
        val findings = mutableListOf<MongoTypeAuditFinding>()
        val failures = mutableListOf<MongoTypeAuditFailure>()
        var complete = true
        var capped = false

        for (target in targets) {
            if (capped) {
                break
            }
            counts[target.collection] = 0L
            try {
                val cursor = collectionProvider(target.collection)
                    .find()
                    .sort(Document("_id", 1))
                    .batchSize(options.batchSize)
                    .iterator()
                cursor.use {
                    while (!capped && it.hasNext()) {
                        val document = it.next()
                        counts[target.collection] = requireNotNull(counts[target.collection]) + 1
                        val documentFindings = try {
                            inspect(target, document)
                        } catch (exception: MongoTypeAuditInspectionException) {
                            complete = false
                            listOf(
                                MongoTypeAuditFinding(
                                    target.collection,
                                    documentId(document["_id"]),
                                    exception.path,
                                    null,
                                    MongoTypeAuditIssue.MALFORMED,
                                    null,
                                )
                            )
                        }
                        for (finding in documentFindings) {
                            if (findings.size == options.maxFindings) {
                                complete = false
                                capped = true
                                break
                            }
                            findings += finding
                        }
                    }
                }
            } catch (exception: Exception) {
                complete = false
                failures += MongoTypeAuditFailure(
                    target.collection,
                    "${exception::class.java.simpleName}: ${exception.message ?: "no details"}",
                )
            }
        }

        return MongoTypeAuditReport(
            findings = findings.sortedWith(FindingOrder),
            scannedDocumentsByCollection = counts.toSortedMap(),
            complete = complete,
            failures = failures.sortedBy { it.collection },
        )
    }

    private fun targets(): List<AuditTarget> = listOf(
        target(WorkflowDocument::class.java) { document, add ->
            auditRoleName(rawField(document, WorkflowDocument::class.java, "modelType"), TypeRole.MODEL, add)
            auditWorkflowModelConsistency(document, add)
        },
        target(StepDocument::class.java) { document, add ->
            auditRoleName(rawField(document, StepDocument::class.java, "kind"), TypeRole.STEP, add, required = true)
            auditLegacyTypeNames(rawField(document, StepDocument::class.java, "dataTypeMap"), add)
            auditLegacyTypeNames(rawField(document, StepDocument::class.java, "metadataTypeMap"), add)
            auditTypedRecords(rawField(document, StepDocument::class.java, "dataEntries"), add)
            auditTypedRecords(rawField(document, StepDocument::class.java, "metadataEntries"), add)
        },
        target(JobDocument::class.java) { document, add ->
            auditRoleName(rawField(document, JobDocument::class.java, "kind"), TypeRole.JOB, add, required = true)
            auditLegacyTypeNames(rawField(document, JobDocument::class.java, "parameterTypeMap"), add)
            auditTypedRecords(rawField(document, JobDocument::class.java, "parameterEntries"), add)
        },
        target(StepDefinitionDocument::class.java) { document, add ->
            auditHistoricalStepKind(rawField(document, StepDefinitionDocument::class.java, "kind"), add)
            auditTypedRecords(rawField(document, StepDefinitionDocument::class.java, "metadata"), add, required = true)
            val data = rawField(document, StepDefinitionDocument::class.java, "data")
            (data.value as? Iterable<*>)?.forEachIndexed { index, entry ->
                val definition = entry as? Map<*, *> ?: return@forEachIndexed
                auditTypedRecords(
                    rawField(definition, DataDefinitionDocument::class.java, "metadata", "${data.path}/$index"),
                    add,
                    required = true,
                )
            }
        },
    )

    private fun target(
        type: Class<*>,
        inspect: (Document, (PendingFinding) -> Unit) -> Unit,
    ): AuditTarget = AuditTarget(
        type,
        access.template.getCollectionName(type),
        inspect,
    )

    private fun inspect(target: AuditTarget, document: Document): List<MongoTypeAuditFinding> {
        val id = documentId(document["_id"])
        val pending = mutableListOf<PendingFinding>()
        val add: (PendingFinding) -> Unit = pending::add

        access.documentTypePolicy.auditAliases(
            target.type,
            document,
            access.valueTypes::isRegistered,
        ).forEach { deviation ->
            add(
                PendingFinding(
                    deviation.path,
                    deviation.expectedRole,
                    when (deviation.issue) {
                        MongoTypeAliasIssue.UNREGISTERED -> MongoTypeAuditIssue.UNREGISTERED
                        MongoTypeAliasIssue.MALFORMED -> MongoTypeAuditIssue.MALFORMED
                        MongoTypeAliasIssue.DISALLOWED -> MongoTypeAuditIssue.DISALLOWED
                    },
                    deviation.persistedName,
                )
            )
        }
        target.inspect(document, add)

        return pending.map { finding ->
            MongoTypeAuditFinding(
                target.collection,
                id,
                finding.path,
                finding.expectedRole,
                finding.issue,
                finding.persistedName,
            )
        }.sortedWith(compareBy { it.path })
    }

    /** Archived definitions retain their kind for restoration without requiring executable registration. */
    private fun auditHistoricalStepKind(field: RawField, add: (PendingFinding) -> Unit) {
        val kind = field.value
        if (kind !is String || kind.isEmpty()) {
            add(invalid(field.path, TypeRole.STEP, kind, knownInAnotherRole = false))
        }
    }

    private fun auditRoleName(
        field: RawField,
        role: TypeRole,
        add: (PendingFinding) -> Unit,
        required: Boolean = false,
    ) {
        if (!field.present) {
            if (required) {
                add(
                    PendingFinding(
                        field.path,
                        role,
                        MongoTypeAuditIssue.MALFORMED,
                        null,
                    )
                )
            }
            return
        }
        val value = field.value
        val allowed = when (role) {
            TypeRole.MODEL -> identifyModelName(value) != null || access.valueTypes.isBuiltIn(value)
            else -> value is String && value in roleNames[role].orEmpty()
        }
        if (!allowed) {
            add(invalid(field.path, role, value, isKnownInAnotherRole(value, role)))
        }
    }

    private fun auditWorkflowModelConsistency(
        document: Map<*, *>,
        add: (PendingFinding) -> Unit,
    ) {
        val modelTypeField = rawField(document, WorkflowDocument::class.java, "modelType")
        val modelType = identifyModelName(modelTypeField.value)
            ?: return
        val model = rawField(document, WorkflowDocument::class.java, "model").value as? Map<*, *> ?: return
        val discriminator = model[access.typeKey]
        val discriminatorType = access.typeAliases.identify(TypeRole.MODEL, discriminator)
            ?: return
        if (modelType != discriminatorType) {
            add(
                PendingFinding(
                    modelTypeField.path,
                    TypeRole.MODEL,
                    MongoTypeAuditIssue.INCONSISTENT,
                    modelTypeField.value as? String,
                )
            )
        }
    }

    private fun auditLegacyTypeNames(
        field: RawField,
        add: (PendingFinding) -> Unit,
    ) {
        // Type-map keys belong to the application; only their values describe infrastructure types.
        val types = field.value as? Map<*, *> ?: return
        types.forEach { (key, value) ->
            val path = "${field.path}/${pointerSegment(key?.toString().orEmpty())}"
            auditInfrastructureTypeNames(RawField(value, true, path), add)
        }
    }

    private fun auditInfrastructureTypeNames(
        field: RawField,
        add: (PendingFinding) -> Unit,
    ) {
        when (val value = field.value) {
            is Map<*, *> -> {
                val typeName = rawField(value, SimpleType::class.java, "typeName", field.path)
                if (typeName.present) {
                    auditValueName(typeName.path, typeName.value, add)
                }
                val literalTypeName = access.fieldNames.fieldParts(SimpleType::class.java, "typeName").singleOrNull()
                value.forEach { (key, nested) ->
                    if (literalTypeName == null || key != literalTypeName) {
                        val nestedPath = "${field.path}/${pointerSegment(key?.toString().orEmpty())}"
                        auditInfrastructureTypeNames(RawField(nested, true, nestedPath), add)
                    }
                }
            }
            is Iterable<*> -> value.forEachIndexed { index, nested ->
                auditInfrastructureTypeNames(RawField(nested, true, "${field.path}/$index"), add)
            }
            is Array<*> -> value.forEachIndexed { index, nested ->
                auditInfrastructureTypeNames(RawField(nested, true, "${field.path}/$index"), add)
            }
        }
    }

    private fun auditTypedRecords(
        field: RawField,
        add: (PendingFinding) -> Unit,
        required: Boolean = false,
    ) {
        if (field.value == null && !required) {
            return
        }
        val records = auditRequiredMap(field, add) ?: return
        auditRequiredMap(rawField(records, TypedRecords::class.java, "values", field.path), add)
        val typesField = rawField(records, TypedRecords::class.java, "types", field.path)
        auditRequiredMap(typesField, add)
        auditLegacyTypeNames(typesField, add)
        val jvmTypesField = rawField(records, TypedRecords::class.java, "jvmTypes", field.path)
        val jvmTypes = auditRequiredMap(jvmTypesField, add) ?: return
        val entriesField = rawField(jvmTypes, JvmTypeMapping::class.java, "entries", jvmTypesField.path)
        if (entriesField.value != null && entriesField.value !is Iterable<*>) {
            add(PendingFinding(entriesField.path, null, MongoTypeAuditIssue.MALFORMED, null))
            return
        }
        val entries = entriesField.value as? Iterable<*> ?: emptyList<Any?>()
        val references = mutableSetOf<String>()
        entries.forEachIndexed { index, entry ->
            val typeEntry = entry as? Map<*, *>
            val entryPath = "${entriesField.path}/$index"
            val type = rawField(typeEntry, TypeRecordEntry::class.java, "type", entryPath)
            auditValueName(
                type.path,
                type.value,
                add,
            )
            if (typeEntry != null) {
                val referenceField = rawField(typeEntry, TypeRecordEntry::class.java, "reference", entryPath)
                val reference = referenceString(referenceField.value)
                if (reference == null || !references.add(reference)) {
                    add(PendingFinding(referenceField.path, null, MongoTypeAuditIssue.MALFORMED, null))
                }
            }
        }
        auditRecordReferences(typesField, references, add)
    }

    /** The document preflight validates the source graph; this focused second pass also enforces its own budget. */
    private fun auditRecordReferences(
        field: RawField,
        references: Set<String>,
        add: (PendingFinding) -> Unit,
    ) {
        val types = field.value as? Map<*, *> ?: return
        val pending = ArrayDeque<RawField>()
        var nodes = 1
        fun enqueue(rawField: RawField) {
            if (rawField.depth > MAX_RECORD_REFERENCE_DEPTH) {
                throw MongoTypeAuditInspectionException(
                    rawField.path,
                    "Mongo document exceeds the maximum depth of $MAX_RECORD_REFERENCE_DEPTH at '${rawField.path}'",
                )
            }
            if (nodes + pending.size >= MAX_RECORD_REFERENCE_NODES) {
                throw MongoTypeAuditInspectionException(
                    rawField.path,
                    "Mongo document exceeds the maximum node count of $MAX_RECORD_REFERENCE_NODES",
                )
            }
            pending.add(rawField)
        }
        types.forEach { (key, value) ->
            enqueue(RawField(value, true, "${field.path}/${pointerSegment(key?.toString().orEmpty())}", depth = 1))
        }
        val visited = IdentityHashMap<Any, Boolean>()
        while (pending.isNotEmpty()) {
            val current = pending.removeLast()
            nodes++
            if (nodes > MAX_RECORD_REFERENCE_NODES) {
                throw MongoTypeAuditInspectionException(
                    current.path,
                    "Mongo document exceeds the maximum node count of $MAX_RECORD_REFERENCE_NODES",
                )
            }
            if (current.depth > MAX_RECORD_REFERENCE_DEPTH) {
                throw MongoTypeAuditInspectionException(
                    current.path,
                    "Mongo document exceeds the maximum depth of $MAX_RECORD_REFERENCE_DEPTH at '${current.path}'",
                )
            }
            val record = current.value as? Map<*, *> ?: continue
            if (visited.put(record, true) != null) {
                continue
            }
            val alias = record[access.typeKey]
            when {
                access.typeAliases.isAliasFor(alias, JvmTypeRecord::class.java) -> {
                    val referenceField = rawField(record, JvmTypeRecord::class.java, "jvmTypeReference", current.path)
                    val reference = auditRequiredMap(referenceField, add) ?: continue
                    val value = rawField(reference, TypeReference::class.java, "value", referenceField.path)
                    val name = referenceString(value.value)
                    if (name == null || name !in references) {
                        add(PendingFinding(value.path, null, MongoTypeAuditIssue.MALFORMED, null))
                    }
                }
                access.typeAliases.isAliasFor(alias, CollectionTypeRecord::class.java) -> {
                    val collectionType = rawField(record, CollectionTypeRecord::class.java, "collectionType", current.path)
                    enqueue(collectionType.copy(depth = current.depth + 1))
                    val components = rawField(record, CollectionTypeRecord::class.java, "componentTypes", current.path)
                    when (val values = components.value) {
                        is Iterable<*> -> values.forEachIndexed { index, value ->
                            enqueue(RawField(value, true, "${components.path}/$index", current.depth + 1))
                        }
                        is Array<*> -> values.forEachIndexed { index, value ->
                            enqueue(RawField(value, true, "${components.path}/$index", current.depth + 1))
                        }
                    }
                }
            }
        }
    }

    private fun referenceString(value: Any?): String? = when (value) {
        is String -> value
        is Boolean, is Byte, is Short, is Int, is Long, is Float, is Double -> value.toString()
        is ObjectId -> value.toHexString()
        is Document -> value.toJson(auditDocumentCodecRegistry.get(Document::class.java))
        else -> null
    }

    private fun auditRequiredMap(field: RawField, add: (PendingFinding) -> Unit): Map<*, *>? {
        val value = field.value as? Map<*, *>
        if (value == null) {
            add(PendingFinding(field.path, null, MongoTypeAuditIssue.MALFORMED, null))
        }
        return value
    }

    private fun identifyModelName(value: Any?): Class<*>? =
        (value as? String)?.let { modelTypesByName[it]?.singleOrNull() }

    private fun auditValueName(path: String, value: Any?, add: (PendingFinding) -> Unit) {
        if (!access.valueTypes.isRegistered(value)) {
            add(invalid(path, TypeRole.VALUE, value, isKnownInAnotherRole(value, TypeRole.VALUE)))
        }
    }

    private fun invalid(
        path: String,
        role: TypeRole,
        value: Any?,
        knownInAnotherRole: Boolean,
    ): PendingFinding = PendingFinding(
        path,
        role,
        when {
            value !is String || value.isEmpty() -> MongoTypeAuditIssue.MALFORMED
            knownInAnotherRole -> MongoTypeAuditIssue.DISALLOWED
            else -> MongoTypeAuditIssue.UNREGISTERED
        },
        value as? String,
    )

    private fun isKnownInAnotherRole(value: Any?, expected: TypeRole): Boolean =
        value is String && access.typeRegistry.entries.any { entry ->
            entry.role != expected && (entry.key == value || entry.binaryClassName == value)
        }

    private fun documentId(value: Any?): String = when (value) {
        is ObjectId -> value.toHexString()
        is String -> value
        null -> "<missing>"
        else -> "<unsupported>"
    }

    private fun pointerSegment(value: String): String = value
        .replace("~", "~0")
        .replace("/", "~1")

    private fun rawField(
        document: Map<*, *>?,
        type: Class<*>,
        property: String,
        parentPath: String = "",
    ): RawField {
        val source = document ?: emptyMap<String, Any?>()
        return RawField(
            access.fieldNames.read(source, type, property),
            access.fieldNames.contains(source, type, property),
            parentPath + access.fieldNames.fieldParts(type, property).joinToString("") { "/${pointerSegment(it)}" },
        )
    }

    private data class RawField(
        val value: Any?,
        val present: Boolean,
        val path: String,
        val depth: Int = 1,
    )

    private data class AuditTarget(
        val type: Class<*>,
        val collection: String,
        val inspect: (Document, (PendingFinding) -> Unit) -> Unit,
    )

    private data class PendingFinding(
        val path: String,
        val expectedRole: TypeRole?,
        val issue: MongoTypeAuditIssue,
        val persistedName: String?,
    )

    private companion object {
        const val MAX_RECORD_REFERENCE_DEPTH = 100
        const val MAX_RECORD_REFERENCE_NODES = 100_000
        private val auditDocumentCodecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(object : Codec<UUID> {
                override fun encode(writer: BsonWriter, value: UUID, encoderContext: EncoderContext) {
                    writer.writeString(value.toString())
                }

                override fun getEncoderClass(): Class<UUID> = UUID::class.java

                override fun decode(reader: BsonReader, decoderContext: DecoderContext): UUID =
                    throw IllegalStateException("decode not supported")
            }),
            MongoClientSettings.getDefaultCodecRegistry(),
        )

        val FindingOrder = compareBy<MongoTypeAuditFinding> { it.collection }
            .thenBy { it.documentId }
            .thenBy { it.path }
            .thenBy { it.expectedRole?.ordinal ?: -1 }
            .thenBy { it.issue.ordinal }
            .thenBy { it.persistedName }
    }
}
