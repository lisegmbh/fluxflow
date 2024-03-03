package de.lise.fluxflow.persistence.job

import de.lise.fluxflow.api.job.JobStatus
import java.time.Instant

data class JobData(
    val id: String,
    val workflowId: String,
    val kind: String,
    val parameters: Map<String, Any?>,
    val scheduledTime: Instant,
    val cancellationKey: String?,
    val status: JobStatus
) {
    fun withStatus(status: JobStatus): JobData {
        return JobData(
            id,
            workflowId,
            kind,
            parameters,
            scheduledTime,
            cancellationKey,
            status
        )
    }
}
