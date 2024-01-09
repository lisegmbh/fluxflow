package de.lise.fluxflow.persistence.continuation.history.query.sort

import de.lise.fluxflow.api.continuation.history.query.sort.ContinuationRecordSort
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.query.sort.Sort
import de.lise.fluxflow.query.sort.UnsupportedSortException

interface ContinuationRecordDataSort : Sort<ContinuationRecordData> {
    companion object {
        fun fromDomainSort(sort: ContinuationRecordSort): ContinuationRecordDataSort {
            return when(sort) {
                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}