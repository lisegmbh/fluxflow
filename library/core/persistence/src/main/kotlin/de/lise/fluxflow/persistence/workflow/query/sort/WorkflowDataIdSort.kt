package de.lise.fluxflow.persistence.workflow.query.sort

import de.lise.fluxflow.api.workflow.query.sort.WorkflowIdSort
import de.lise.fluxflow.query.sort.Direction

data class WorkflowDataIdSort(override val direction: Direction) : WorkflowDataSort {
    constructor(idSort: WorkflowIdSort<*>) : this(idSort.direction)
}