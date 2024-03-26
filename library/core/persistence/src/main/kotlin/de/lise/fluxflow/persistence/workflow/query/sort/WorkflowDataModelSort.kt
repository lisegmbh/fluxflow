package de.lise.fluxflow.persistence.workflow.query.sort

import de.lise.fluxflow.api.workflow.query.sort.WorkflowModelSort
import de.lise.fluxflow.query.sort.Sort

data class WorkflowDataModelSort(
    val sort: Sort<*>
) : WorkflowDataSort {
    constructor(modelSort: WorkflowModelSort<*>) : this(modelSort.modelSort)
}