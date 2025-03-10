package de.lise.fluxflow.mongo.job

import de.lise.fluxflow.persistence.job.query.JobDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.PaginationRequest
import org.springframework.data.domain.Sort

class JobDocumentQuery(
    filter: JobDocumentFilter?,
    sort: List<Sort>,
    page: PaginationRequest?
) : Query<JobDocumentFilter, Sort>(filter, sort, page) {
    constructor(query: JobDataQuery) : this(
        query.filter?.let { JobDocumentFilter(it) },
        query.sort.map { JobDocumentSort.toMongoSort(it) },
        query.page
    )
}
