package de.lise.fluxflow.api.workflow.query.sort

import de.lise.fluxflow.query.sort.Direction
import de.lise.fluxflow.query.sort.Sort

data class WorkflowModelSort<TWorkflowModel>(
    val modelSort: Sort<TWorkflowModel>
) : WorkflowSort<TWorkflowModel> {
    override val direction: Direction
        get() = modelSort.direction
}