package de.lise.fluxflow.api.workflow.query.sort

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.query.sort.Direction
import de.lise.fluxflow.query.sort.Sort

interface WorkflowSort<TWorkflowModel> : Sort<Workflow<TWorkflowModel>> {
    companion object {
        fun <TWorkflowModel> id(direction: Direction): WorkflowSort<TWorkflowModel> {
            return WorkflowIdSort(direction)
        }

        fun <TWorkflowModel> model(sort: Sort<TWorkflowModel>): WorkflowSort<TWorkflowModel> {
            return WorkflowModelSort(sort)
        }
    }
}

