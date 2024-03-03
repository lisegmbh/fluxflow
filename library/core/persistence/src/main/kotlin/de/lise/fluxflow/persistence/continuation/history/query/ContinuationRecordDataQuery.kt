package de.lise.fluxflow.persistence.continuation.history.query

import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQuery
import de.lise.fluxflow.persistence.continuation.history.query.filter.ContinuationRecordDataFilter
import de.lise.fluxflow.persistence.continuation.history.query.sort.ContinuationRecordDataSort
import de.lise.fluxflow.query.Query

typealias ContinuationRecordDataQuery = Query<ContinuationRecordDataFilter, ContinuationRecordDataSort>

fun ContinuationRecordQuery.toDataQuery(): ContinuationRecordDataQuery {
    return ContinuationRecordDataQuery(
        this.filter?.let { ContinuationRecordDataFilter(it) },
        this.sort.map { ContinuationRecordDataSort.fromDomainSort(it) },
        this.page
    )
}