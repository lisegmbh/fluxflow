package de.lise.fluxflow.test.persistence.job

import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.query.filter.JobDataFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate
import java.time.Instant

class JobTestFilter(
    private val id: InMemoryFilter<String>?,
    private val workflowId: InMemoryFilter<String>?,
    private val kind: InMemoryFilter<String>?,
    private val scheduledTime: InMemoryFilter<Instant>?,
    private val status: InMemoryFilter<JobStatus>?,
) {
    constructor(it: JobDataFilter) : this(
        it.id?.let { InMemoryFilter.fromDomainFilter(it) },
        it.workflowId?.let { InMemoryFilter.fromDomainFilter(it) },
        it.kind?.let { InMemoryFilter.fromDomainFilter(it) },
        it.scheduledTime?.let { InMemoryFilter.fromDomainFilter(it) },
        it.status?.let { InMemoryFilter.fromDomainFilter(it) },
    )

    fun toPredicate(): InMemoryPredicate<JobData> {
        var result = InMemoryPredicate<JobData> {
            true
        }

        if (id != null) {
            result = result.and(id.toPredicate().mapping { it.id })
        }
        if (workflowId != null) {
            result = result.and(workflowId.toPredicate().mapping { it.workflowId })
        }
        if (kind != null) {
            result = result.and(kind.toPredicate().mapping { it.kind })
        }
        if (scheduledTime != null) {
            result = result.and(scheduledTime.toPredicate().mapping { it.scheduledTime })
        }
        if (status != null) {
            result = result.and(status.toPredicate().mapping { it.status })
        }

        return result
    }
}
