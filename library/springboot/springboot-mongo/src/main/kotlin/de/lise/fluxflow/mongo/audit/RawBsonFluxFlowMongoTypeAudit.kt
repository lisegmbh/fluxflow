package de.lise.fluxflow.mongo.audit

import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.MongoTypeAliasIssue
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.definition.StepDefinitionDocument
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.reflection.types.TypeRole
import org.bson.Document
import org.bson.types.ObjectId

/** Uses the synchronous driver directly so the scan never invokes the mapping converter. */
internal class RawBsonFluxFlowMongoTypeAudit(
    private val access: FluxFlowMongoAccess,
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
                val cursor = access.template.getCollection(target.collection)
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
            auditRoleName(document, "modelType", TypeRole.MODEL, add)
            auditWorkflowModelConsistency(document, add)
        },
        target(StepDocument::class.java) { document, add ->
            auditRoleName(document, "kind", TypeRole.STEP, add, required = true)
            auditLegacyTypeNames(document["dataTypeMap"], "/dataTypeMap", add)
            auditLegacyTypeNames(document["metadataTypeMap"], "/metadataTypeMap", add)
            auditTypedRecords(document["dataEntries"], "/dataEntries", add)
            auditTypedRecords(document["metadataEntries"], "/metadataEntries", add)
        },
        target(JobDocument::class.java) { document, add ->
            auditRoleName(document, "kind", TypeRole.JOB, add, required = true)
            auditLegacyTypeNames(document["parameterTypeMap"], "/parameterTypeMap", add)
            auditTypedRecords(document["parameterEntries"], "/parameterEntries", add)
        },
        target(StepDefinitionDocument::class.java) { document, add ->
            auditRoleName(document, "kind", TypeRole.STEP, add, required = true)
            auditTypedRecords(document["metadata"], "/metadata", add)
            (document["data"] as? Iterable<*>)?.forEachIndexed { index, entry ->
                val definition = entry as? Map<*, *> ?: return@forEachIndexed
                auditTypedRecords(definition["metadata"], "/data/$index/metadata", add)
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
        document: Map<*, *>,
        field: String,
        role: TypeRole,
        add: (PendingFinding) -> Unit,
        required: Boolean = false,
    ) {
        if (!document.containsKey(field)) {
            if (required) {
                add(
                    PendingFinding(
                        "/$field",
                        role,
                        MongoTypeAuditIssue.MALFORMED,
                        null,
                    )
                )
            }
            return
        }
        val value = document[field]
        val allowed = when (role) {
            TypeRole.MODEL -> identifyModelName(value) != null || access.valueTypes.isBuiltIn(value)
            else -> value is String && value in roleNames[role].orEmpty()
        }
        if (!allowed) {
            add(invalid("/$field", role, value, isKnownInAnotherRole(value, role)))
        }
    }

    private fun auditWorkflowModelConsistency(
        document: Map<*, *>,
        add: (PendingFinding) -> Unit,
    ) {
        val modelType = identifyModelName(document["modelType"])
            ?: return
        val model = document["model"] as? Map<*, *> ?: return
        val discriminator = model[access.typeKey]
        val discriminatorType = access.typeAliases.identify(TypeRole.MODEL, discriminator)
            ?: return
        if (modelType != discriminatorType) {
            add(
                PendingFinding(
                    "/modelType",
                    TypeRole.MODEL,
                    MongoTypeAuditIssue.INCONSISTENT,
                    document["modelType"] as? String,
                )
            )
        }
    }

    private fun auditLegacyTypeNames(
        value: Any?,
        path: String,
        add: (PendingFinding) -> Unit,
    ) {
        when (value) {
            is Map<*, *> -> value.forEach { (key, nested) ->
                val nestedPath = "$path/${pointerSegment(key?.toString().orEmpty())}"
                if (key == "typeName") {
                    auditValueName(nestedPath, nested, add)
                } else {
                    auditLegacyTypeNames(nested, nestedPath, add)
                }
            }
            is Iterable<*> -> value.forEachIndexed { index, nested ->
                auditLegacyTypeNames(nested, "$path/$index", add)
            }
            is Array<*> -> value.forEachIndexed { index, nested ->
                auditLegacyTypeNames(nested, "$path/$index", add)
            }
        }
    }

    private fun auditTypedRecords(
        value: Any?,
        path: String,
        add: (PendingFinding) -> Unit,
    ) {
        val records = value as? Map<*, *> ?: return
        val jvmTypes = records["jvmTypes"] as? Map<*, *> ?: return
        val entries = jvmTypes["entries"] as? Iterable<*> ?: return
        entries.forEachIndexed { index, entry ->
            val typeEntry = entry as? Map<*, *>
            auditValueName(
                "$path/jvmTypes/entries/$index/type",
                typeEntry?.get("type"),
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
