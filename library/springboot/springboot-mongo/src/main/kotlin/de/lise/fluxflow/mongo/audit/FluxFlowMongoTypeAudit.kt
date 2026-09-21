package de.lise.fluxflow.mongo.audit

import de.lise.fluxflow.reflection.types.TypeRole

/** Explicitly scans persisted FluxFlow documents for type names outside the trusted registry. */
interface FluxFlowMongoTypeAudit {
    fun audit(options: MongoTypeAuditOptions = MongoTypeAuditOptions()): MongoTypeAuditReport
}

data class MongoTypeAuditOptions(
    val batchSize: Int = 500,
    val maxFindings: Int = 10_000,
) {
    init {
        require(batchSize > 0) { "Mongo audit batch size must be positive" }
        require(maxFindings > 0) { "Mongo audit maximum finding count must be positive" }
    }
}

data class MongoTypeAuditReport(
    val findings: List<MongoTypeAuditFinding>,
    val scannedDocumentsByCollection: Map<String, Long>,
    val complete: Boolean,
    val failures: List<MongoTypeAuditFailure> = emptyList(),
) {
    val isCompatible: Boolean
        get() = complete && findings.isEmpty() && failures.isEmpty()
}

data class MongoTypeAuditFinding(
    val collection: String,
    val documentId: String,
    val path: String,
    val expectedRole: TypeRole?,
    val issue: MongoTypeAuditIssue,
    val persistedName: String?,
)

enum class MongoTypeAuditIssue {
    UNREGISTERED,
    MALFORMED,
    DISALLOWED,
    INCONSISTENT,
}

data class MongoTypeAuditFailure(
    val collection: String,
    val cause: String,
)
