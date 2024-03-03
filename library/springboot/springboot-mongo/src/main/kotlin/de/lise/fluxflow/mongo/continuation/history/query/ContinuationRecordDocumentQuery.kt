package de.lise.fluxflow.mongo.continuation.history.query

import de.lise.fluxflow.mongo.continuation.history.query.filter.ContinuationRecordDocumentFilter
import de.lise.fluxflow.mongo.continuation.history.query.sort.ContinuationRecordDocumentSort
import de.lise.fluxflow.persistence.continuation.history.query.ContinuationRecordDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.PaginationRequest
import org.springframework.data.domain.Sort

class ContinuationRecordDocumentQuery(
    filter: ContinuationRecordDocumentFilter?,
    sort: List<Sort>,
    page: PaginationRequest?,
): Query<ContinuationRecordDocumentFilter, Sort>(filter, sort, page) {
    constructor(
        query: ContinuationRecordDataQuery
    ) : this(
        query.filter?.let { ContinuationRecordDocumentFilter(it) },
        query.sort.map { ContinuationRecordDocumentSort.toMongoSort(it) },
        query.page
    )
}