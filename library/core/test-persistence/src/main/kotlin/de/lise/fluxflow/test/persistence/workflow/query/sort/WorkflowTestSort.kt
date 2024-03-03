package de.lise.fluxflow.test.persistence.workflow.query.sort

import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.query.sort.WorkflowDataIdSort
import de.lise.fluxflow.persistence.workflow.query.sort.WorkflowDataModelSort
import de.lise.fluxflow.persistence.workflow.query.sort.WorkflowDataSort
import de.lise.fluxflow.query.inmemory.sort.InMemoryDefaultSort
import de.lise.fluxflow.query.inmemory.sort.InMemoryPropertySort
import de.lise.fluxflow.query.inmemory.sort.InMemorySort
import de.lise.fluxflow.query.sort.UnsupportedSortException

interface WorkflowTestSort {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun toTestSort(
            sort: WorkflowDataSort
        ): InMemorySort<WorkflowData> {
            return when (sort) {
                is WorkflowDataIdSort -> InMemoryPropertySort(
                    WorkflowData::id,
                    InMemoryDefaultSort(sort.direction)
                )

                is WorkflowDataModelSort -> InMemoryPropertySort(
                    WorkflowData::model,
                    InMemorySort.toTestSort(sort.sort) as InMemorySort<Any?>
                )

                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}