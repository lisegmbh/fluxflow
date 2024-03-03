package de.lise.fluxflow.persistence.workflow.query.sort

import de.lise.fluxflow.api.workflow.query.sort.WorkflowIdSort
import de.lise.fluxflow.api.workflow.query.sort.WorkflowModelSort
import de.lise.fluxflow.api.workflow.query.sort.WorkflowSort
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.query.sort.Sort
import de.lise.fluxflow.query.sort.UnsupportedSortException

interface WorkflowDataSort : Sort<WorkflowData> {
    companion object {
        fun fromDomainSort(sort: WorkflowSort<*>): WorkflowDataSort {
            return when (sort) {
                is WorkflowIdSort<*> -> WorkflowDataIdSort(sort)
                is WorkflowModelSort<*> -> WorkflowDataModelSort(sort)
                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}

