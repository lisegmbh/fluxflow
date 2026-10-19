package de.lise.fluxflow.mongo.audit

import com.mongodb.client.MongoCollection
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.MongoTypeAliasIssue
import de.lise.fluxflow.mongo.generic.SimpleType
import de.lise.fluxflow.mongo.generic.record.JvmTypeMapping
import de.lise.fluxflow.mongo.generic.record.TypeRecordEntry
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.DataDefinitionDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeRole
import org.bson.Document
import org.bson.types.ObjectId

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
        val counts = targets.associateTo(linkedMapOf()) { it.collection to 0L }
        val findings = mutableListOf<MongoTypeAuditFinding>()
        val failures = mutableListOf<MongoTypeAuditFailure>()
        var complete = true
        var capped = false

        for (target in targets) {
            if (capped) {
                break
            }
            try {
                val cursor = collectionProvider(target.collection)
                    .find()
                    .sort(Document("_id", 1))
                    .batchSize(options.batchSize)
                    .iterator()
                cursor.use {
                    while (it.hasNext() && !capped) {
                        val document = it.next()
                        counts[target.collection] = requireNotNull(counts[target.collection]) + 1
                        val documentFindings = inspect(target, document)
                        for (finding in documentFindings) {
                            findings += finding
                            if (findings.size == options.maxFindings) {
                                complete = false
                                capped = true
                                break
                            }
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
            auditRoleName(rawField(document, StepDefinitionDocument::class.java, "kind"), TypeRole.STEP, add, required = true)
            auditTypedRecords(rawField(document, StepDefinitionDocument::class.java, "metadata"), add)
            val data = rawField(document, StepDefinitionDocument::class.java, "data")
            (data.value as? Iterable<*>)?.forEachIndexed { index, entry ->
                val definition = entry as? Map<*, *> ?: return@forEachIndexed
                auditTypedRecords(
                    rawField(definition, DataDefinitionDocument::class.java, "metadata", "${data.path}/$index"),
                    add,
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
                        auditLegacyTypeNames(RawField(nested, true, nestedPath), add)
                    }
                }
            }
            is Iterable<*> -> value.forEachIndexed { index, nested ->
                auditLegacyTypeNames(RawField(nested, true, "${field.path}/$index"), add)
            }
            is Array<*> -> value.forEachIndexed { index, nested ->
                auditLegacyTypeNames(RawField(nested, true, "${field.path}/$index"), add)
            }
        }
    }

    private fun auditTypedRecords(
        field: RawField,
        add: (PendingFinding) -> Unit,
    ) {
        val records = field.value as? Map<*, *> ?: return
        val jvmTypesField = rawField(records, TypedRecords::class.java, "jvmTypes", field.path)
        val jvmTypes = jvmTypesField.value as? Map<*, *> ?: return
        val entriesField = rawField(jvmTypes, JvmTypeMapping::class.java, "entries", jvmTypesField.path)
        val entries = entriesField.value as? Iterable<*> ?: return
        entries.forEachIndexed { index, entry ->
            val type = rawField(entry as? Map<*, *>, TypeRecordEntry::class.java, "type", "${entriesField.path}/$index")
            auditValueName(
                type.path,
                type.value,
                add,
            )
        }
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
        val FindingOrder = compareBy<MongoTypeAuditFinding> { it.collection }
            .thenBy { it.documentId }
            .thenBy { it.path }
            .thenBy { it.expectedRole?.ordinal ?: -1 }
            .thenBy { it.issue.ordinal }
            .thenBy { it.persistedName }
    }
}
