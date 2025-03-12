package de.lise.fluxflow.mongo.job

import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.mongo.query.filter.DocumentFilter
import de.lise.fluxflow.mongo.query.filter.MongoFilter
import de.lise.fluxflow.persistence.job.query.filter.JobDataFilter
import org.springframework.data.mongodb.core.query.Criteria
import java.time.Instant

class JobDocumentFilter(
    private val id: MongoFilter<String>?,
    private val workflowId: MongoFilter<String>?,
    private val kind: MongoFilter<String>?,
    private val scheduledTime: MongoFilter<Instant>?,
    private val status: MongoFilter<JobStatus>?,
) : DocumentFilter<JobDocument> {
    constructor(jobDataFilter: JobDataFilter) : this(
        jobDataFilter.id?.let { MongoFilter.fromDomainFilter(it) },
        jobDataFilter.workflowId?.let { MongoFilter.fromDomainFilter(it) },
        jobDataFilter.kind?.let { MongoFilter.fromDomainFilter(it) },
        jobDataFilter.scheduledTime?.let { MongoFilter.fromDomainFilter(it) },
        jobDataFilter.status?.let { MongoFilter.fromDomainFilter(it) }
    )

    override fun toCriteria(): Criteria {
        val criteria = mutableListOf<Criteria>()

        if (id != null) {
            criteria.add(id.apply(JobDocument::id.name))
        }
        if (workflowId != null) {
            criteria.add(workflowId.apply(JobDocument::workflowId.name))
        }
        if (kind != null) {
            criteria.add(kind.apply(JobDocument::kind.name))
        }
        if (scheduledTime != null) {
            criteria.add(scheduledTime.apply(JobDocument::scheduledTime.name))
        }
        if (status != null) {
            criteria.add(status.apply(JobDocument::jobStatus.name))
        }

        return when (criteria.size) {
            0 -> Criteria()
            1 -> criteria.first()
            else -> Criteria().andOperator(criteria)
        }
    }
}
