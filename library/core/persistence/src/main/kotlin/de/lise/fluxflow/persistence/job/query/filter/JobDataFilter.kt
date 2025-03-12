package de.lise.fluxflow.persistence.job.query.filter

import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.job.query.filter.JobFilter
import de.lise.fluxflow.query.filter.Filter
import java.time.Instant

data class JobDataFilter(
    val id: Filter<String>?,
    val workflowId: Filter<String>?,
    val kind: Filter<String>?,
    val scheduledTime: Filter<Instant>?,
    val status: Filter<JobStatus>?,
) {
    constructor(jobFilter: JobFilter) : this(
        jobFilter.id,
        jobFilter.workflowId,
        jobFilter.kind,
        jobFilter.scheduledTime,
        jobFilter.status
    )
}
