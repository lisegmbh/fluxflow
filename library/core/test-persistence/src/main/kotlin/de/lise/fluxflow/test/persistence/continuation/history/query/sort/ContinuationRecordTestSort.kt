package de.lise.fluxflow.test.persistence.continuation.history.query.sort

import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.query.sort.ContinuationRecordDataSort
import de.lise.fluxflow.query.inmemory.sort.InMemorySort
import de.lise.fluxflow.query.sort.UnsupportedSortException

interface ContinuationRecordTestSort {
    companion object {
        fun toTestSort(
            sort: ContinuationRecordDataSort
        ): InMemorySort<ContinuationRecordData> {
            return when(sort) {

                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}