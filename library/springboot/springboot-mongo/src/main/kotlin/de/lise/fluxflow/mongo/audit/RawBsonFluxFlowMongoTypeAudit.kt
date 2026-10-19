package de.lise.fluxflow.mongo.audit

import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoCollection
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.MongoTypeAuditInspectionException
import de.lise.fluxflow.mongo.MongoTypeViolation
import de.lise.fluxflow.mongo.pointerSegment
import de.lise.fluxflow.mongo.generic.record.CollectionTypeRecord
import de.lise.fluxflow.mongo.generic.record.JvmTypeRecord
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.NullTypeRecord
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
            auditTypedRecords(rawField(document, StepDocument::class.java, "dataEntries"), add)
            auditTypedRecords(rawField(document, StepDocument::class.java, "metadataEntries"), add)
        },
        target(JobDocument::class.java) { document, add ->
            auditRoleName(rawField(document, JobDocument::class.java, "kind"), TypeRole.JOB, add, required = true)
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
        inspect: (Document, (MongoTypeViolation) -> Unit) -> Unit,
    ): AuditTarget = AuditTarget(
        type,
        access.template.getCollectionName(type),
        inspect,
    )

    private fun inspect(target: AuditTarget, document: Document): List<MongoTypeAuditFinding> {
        val id = documentId(document["_id"])
        val pending = mutableListOf<MongoTypeViolation>()
        val add: (MongoTypeViolation) -> Unit = pending::add

        access.documentTypePolicy.auditAliases(
            target.type,
            document,
            access.valueTypes::isRegistered,
            { value -> isKnownInAnotherRole(value, TypeRole.VALUE) },
        ).forEach(add)
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
    private fun auditHistoricalStepKind(field: RawField, add: (MongoTypeViolation) -> Unit) {
        val kind = field.value
        if (kind !is String || kind.isEmpty()) {
            add(invalid(field.path, TypeRole.STEP, kind, knownInAnotherRole = false))
        }
    }

    private fun auditRoleName(
        field: RawField,
        role: TypeRole,
        add: (MongoTypeViolation) -> Unit,
        required: Boolean = false,
    ) {
        if (!field.present) {
            if (required) {
                add(
                    MongoTypeViolation(
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
        add: (MongoTypeViolation) -> Unit,
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
                MongoTypeViolation(
                    modelTypeField.path,
                    TypeRole.MODEL,
                    MongoTypeAuditIssue.INCONSISTENT,
                    modelTypeField.value as? String,
                )
            )
        }
    }

    private fun auditTypedRecords(
        field: RawField,
        add: (MongoTypeViolation) -> Unit,
        required: Boolean = false,
    ) {
        if (field.value == null && !required) {
            return
        }
        val records = auditRequiredMap(field, add) ?: return
        val valuesField = rawField(records, TypedRecords::class.java, "values", field.path)
        val values = auditRequiredMap(valuesField, add)
        val typesField = rawField(records, TypedRecords::class.java, "types", field.path)
        val types = auditRequiredMap(typesField, add)
        if (values != null && types != null) {
            (values.keys - types.keys).forEach { key ->
                add(
                    MongoTypeViolation(
                        "${typesField.path}/${pointerSegment(key?.toString().orEmpty())}",
                        null,
                        MongoTypeAuditIssue.MALFORMED,
                        null,
                    )
                )
            }
            (types.keys - values.keys).forEach { key ->
                add(
                    MongoTypeViolation(
                        "${valuesField.path}/${pointerSegment(key?.toString().orEmpty())}",
                        null,
                        MongoTypeAuditIssue.MALFORMED,
                        null,
                    )
                )
            }
        }
        val jvmTypesField = rawField(records, TypedRecords::class.java, "jvmTypes", field.path)
        val jvmTypes = auditRequiredMap(jvmTypesField, add) ?: return
        val entriesField = rawField(jvmTypes, JvmTypeMapping::class.java, "entries", jvmTypesField.path)
        if (entriesField.value != null && entriesField.value !is Iterable<*>) {
            add(MongoTypeViolation(entriesField.path, null, MongoTypeAuditIssue.MALFORMED, null))
            return
        }
        val entries = entriesField.value as? Iterable<*> ?: emptyList<Any?>()
        val references = mutableSetOf<String>()
        entries.forEachIndexed { index, entry ->
            val typeEntry = entry as? Map<*, *>
            val entryPath = "${entriesField.path}/$index"
            if (typeEntry != null) {
                val referenceField = rawField(typeEntry, TypeRecordEntry::class.java, "reference", entryPath)
                val reference = referenceString(referenceField.value)
                if (reference == null || !references.add(reference)) {
                    add(MongoTypeViolation(referenceField.path, null, MongoTypeAuditIssue.MALFORMED, null))
                }
            }
        }
        auditRecordReferences(typesField, values, references, add)
    }

    /** The document preflight validates the source graph; this focused second pass also enforces its own budget. */
    private fun auditRecordReferences(
        field: RawField,
        values: Map<*, *>?,
        references: Set<String>,
        add: (MongoTypeViolation) -> Unit,
    ) {
        val types = field.value as? Map<*, *> ?: return
        data class RecordFrame(val field: RawField, val payload: Any?, val payloadPresent: Boolean)
        val pending = ArrayDeque<RecordFrame>()
        var nodes = 1
        fun enqueue(rawField: RawField, payload: Any? = null, payloadPresent: Boolean = false) {
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
            pending.add(RecordFrame(rawField, payload, payloadPresent))
        }
        types.forEach { (key, value) ->
            enqueue(
                RawField(value, true, "${field.path}/${pointerSegment(key?.toString().orEmpty())}", depth = 1),
                values?.get(key),
                values?.containsKey(key) == true,
            )
        }
        val visitedRecords = IdentityHashMap<Any, Boolean>()
        val visitedPayloadPairs = IdentityHashMap<Any, IdentityHashMap<Any, Boolean>>()
        while (pending.isNotEmpty()) {
            val frame = pending.removeLast()
            val current = frame.field
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
            val record = current.value as? Map<*, *>
            if (record == null) {
                add(MongoTypeViolation(current.path, null, MongoTypeAuditIssue.MALFORMED, null))
                continue
            }
            val firstRecordVisit = visitedRecords.put(record, true) == null
            val payloadCollection = if (frame.payloadPresent) frame.payload as? Collection<*> else null
            val newPayloadPair = if (payloadCollection == null) {
                false
            } else {
                visitedPayloadPairs.getOrPut(record) { IdentityHashMap() }.put(payloadCollection, true) == null
            }
            if (!firstRecordVisit && !newPayloadPair) {
                continue
            }
            if (firstRecordVisit && !record.containsKey(access.typeKey)) {
                add(
                    MongoTypeViolation(
                        "${current.path}/${pointerSegment(access.typeKey)}",
                        null,
                        MongoTypeAuditIssue.MALFORMED,
                        null,
                    )
                )
            }
            val alias = record[access.typeKey]
            when {
                firstRecordVisit && access.typeAliases.isInfrastructure(alias) &&
                    !access.typeAliases.isAliasFor(alias, JvmTypeRecord::class.java) &&
                    !access.typeAliases.isAliasFor(alias, CollectionTypeRecord::class.java) &&
                    !access.typeAliases.isAliasFor(alias, NullTypeRecord::class.java) -> {
                    add(
                        MongoTypeViolation(
                            "${current.path}/${pointerSegment(access.typeKey)}",
                            null,
                            MongoTypeAuditIssue.MALFORMED,
                            alias as? String,
                        )
                    )
                }
                firstRecordVisit && access.typeAliases.isAliasFor(alias, JvmTypeRecord::class.java) -> {
                    val referenceField = rawField(record, JvmTypeRecord::class.java, "jvmTypeReference", current.path)
                    val reference = auditRequiredMap(referenceField, add) ?: continue
                    val value = rawField(reference, TypeReference::class.java, "value", referenceField.path)
                    val name = referenceString(value.value)
                    if (name == null || name !in references) {
                        add(MongoTypeViolation(value.path, null, MongoTypeAuditIssue.MALFORMED, null))
                    }
                }
                access.typeAliases.isAliasFor(alias, CollectionTypeRecord::class.java) -> {
                    if (firstRecordVisit) {
                        val collectionType = rawField(record, CollectionTypeRecord::class.java, "collectionType", current.path)
                        if (auditRequiredMap(collectionType, add) != null) {
                            enqueue(collectionType.copy(depth = current.depth + 1))
                        }
                    }
                    val components = rawField(record, CollectionTypeRecord::class.java, "componentTypes", current.path)
                    var componentCount = 0
                    val valueIterator = payloadCollection?.iterator()
                    fun enqueueComponent(index: Int, component: Any?) {
                        val hasPayload = valueIterator?.hasNext() == true
                        val nestedPayload = if (hasPayload) valueIterator?.next() else null
                        enqueue(
                            RawField(component, true, "${components.path}/$index", current.depth + 1),
                            nestedPayload,
                            hasPayload,
                        )
                        componentCount++
                    }
                    when (val componentValues = components.value) {
                        is Iterable<*> -> componentValues.forEachIndexed { index, component ->
                            enqueueComponent(index, component)
                        }
                        is Array<*> -> componentValues.forEachIndexed { index, component ->
                            enqueueComponent(index, component)
                        }
                        else -> if (firstRecordVisit) {
                            add(MongoTypeViolation(components.path, null, MongoTypeAuditIssue.MALFORMED, null))
                        }
                    }
                    if (payloadCollection != null &&
                        (components.value is Iterable<*> || components.value is Array<*>)
                    ) {
                        if (payloadCollection.size != componentCount) {
                            add(MongoTypeViolation(components.path, null, MongoTypeAuditIssue.MALFORMED, null))
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

    private fun auditRequiredMap(field: RawField, add: (MongoTypeViolation) -> Unit): Map<*, *>? {
        val value = field.value as? Map<*, *>
        if (value == null) {
            add(MongoTypeViolation(field.path, null, MongoTypeAuditIssue.MALFORMED, null))
        }
        return value
    }

    private fun identifyModelName(value: Any?): Class<*>? =
        (value as? String)?.let { modelTypesByName[it]?.singleOrNull() }

    private fun invalid(
        path: String,
        role: TypeRole,
        value: Any?,
        knownInAnotherRole: Boolean,
    ): MongoTypeViolation = MongoTypeViolation(
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
        val inspect: (Document, (MongoTypeViolation) -> Unit) -> Unit,
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
