package de.lise.fluxflow.mongo.workflow.query.sort

import de.lise.fluxflow.mongo.query.sort.MongoDefaultSort
import de.lise.fluxflow.mongo.query.sort.MongoSort
import de.lise.fluxflow.persistence.workflow.query.sort.WorkflowDataIdSort
import de.lise.fluxflow.persistence.workflow.query.sort.WorkflowDataModelSort
import de.lise.fluxflow.persistence.workflow.query.sort.WorkflowDataSort
import de.lise.fluxflow.query.sort.UnsupportedSortException
import org.springframework.data.domain.Sort

interface WorkflowDocumentSort {
    companion object {
        fun toMongoSort(
            sort: WorkflowDataSort
        ): Sort {
            return when (sort) {
                is WorkflowDataIdSort -> MongoDefaultSort<String>(sort.direction).apply("id")
                is WorkflowDataModelSort -> MongoSort.toMongoSort(sort.sort).apply("model")
                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}