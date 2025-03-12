package de.lise.fluxflow.api.job.query.filter

import de.lise.fluxflow.api.Api
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.query.filter.Filter
import java.time.Instant

data class JobFilter(
    val id: Filter<String>?,
    val workflowId: Filter<String>?,
    val kind: Filter<String>?,
    val scheduledTime: Filter<Instant>?,
    val status: Filter<JobStatus>?,
) {
    @Api
    fun withId(id: Filter<String>): JobFilter {
        return JobFilter(
            id,
            workflowId,
            kind,
            scheduledTime,
            status
        )
    }

    @Api
    fun withWorkflowId(workflowId: Filter<String>): JobFilter {
        return JobFilter(
            id,
            workflowId,
            kind,
            scheduledTime,
            status
        )
    }

    @Api
    fun withKind(kind: Filter<String>): JobFilter {
        return JobFilter(
            id,
            workflowId,
            kind,
            scheduledTime,
            status
        )
    }

    @Api
    fun withScheduledTime(scheduledTime: Filter<Instant>): JobFilter {
        return JobFilter(
            id,
            workflowId,
            kind,
            scheduledTime,
            status
        )
    }

    @Api
    fun withStatus(status: Filter<JobStatus>): JobFilter {
        return JobFilter(
            id,
            workflowId,
            kind,
            scheduledTime,
            status
        )
    }

    companion object {
        fun empty(): JobFilter {
            return JobFilter(
                null,
                null,
                null,
                null,
                null,
            )
        }
    }
}
