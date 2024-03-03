package de.lise.fluxflow.mongo.step.query.sort

import de.lise.fluxflow.mongo.query.sort.MongoDefaultSort
import de.lise.fluxflow.persistence.step.query.sort.StepDataIdSort
import de.lise.fluxflow.persistence.step.query.sort.StepDataSort
import de.lise.fluxflow.query.sort.UnsupportedSortException
import org.springframework.data.domain.Sort

interface StepDocumentSort {
    companion object {
        fun toMongoSort(
            sort: StepDataSort
        ): Sort {
            return when (sort) {
                is StepDataIdSort -> MongoDefaultSort<String>(sort.direction).apply("id")
                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}
