package de.lise.fluxflow.api.job.query

import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobKind
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import java.time.Instant

interface JobQueryable {
    val identifier: JobIdentifier
    val workflowIdentifier: WorkflowIdentifier
    val kind: JobKind
    val parameters: Map<String, Any?>
    val scheduledTime: Instant
    val cancellationKey: String?
    val status: JobStatus
}