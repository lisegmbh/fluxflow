package de.lise.fluxflow.mongo.continuation.history.query.sort

import de.lise.fluxflow.persistence.continuation.history.query.sort.ContinuationRecordDataSort
import de.lise.fluxflow.query.sort.UnsupportedSortException
import org.springframework.data.domain.Sort

interface ContinuationRecordDocumentSort {
    companion object {
        fun toMongoSort(
            sort: ContinuationRecordDataSort
        ): Sort {
            return when(sort) {

                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}