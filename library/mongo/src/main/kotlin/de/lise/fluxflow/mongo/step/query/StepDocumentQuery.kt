package de.lise.fluxflow.mongo.step.query

import de.lise.fluxflow.mongo.step.query.filter.StepDocumentFilter
import de.lise.fluxflow.mongo.step.query.sort.StepDocumentSort
import de.lise.fluxflow.persistence.step.query.StepDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.PaginationRequest
import org.springframework.data.domain.Sort

class StepDocumentQuery(
    filter: StepDocumentFilter?,
    sort: List<Sort>,
    page: PaginationRequest?
) : Query<StepDocumentFilter, Sort>(filter, sort, page) {
    constructor(
        query: StepDataQuery
    ) : this(
        query.filter?.let { StepDocumentFilter(it) },
        query.sort.map { StepDocumentSort.toMongoSort(it) },
        query.page
    )
}