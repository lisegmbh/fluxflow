package de.lise.fluxflow.persistence.job

import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier

/**
 * Identifies a scheduled job without materializing its persisted payload.
 */
data class ScheduledJobReference(
    val workflowIdentifier: WorkflowIdentifier,
    val jobIdentifier: JobIdentifier,
)

/**
 * Enumerates scheduled jobs using only their persisted identifiers and status.
 */
fun interface ScheduledJobReferencePersistence {
    fun findScheduledJobReferences(): List<ScheduledJobReference>
}
