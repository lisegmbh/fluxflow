package de.lise.fluxflow.api.workflow

import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.workflow.action.WorkflowAction

interface WorkflowActionService {
    fun <TModel : Any> getActions(workflow: Workflow<TModel>): List<WorkflowAction<TModel>>
    fun <TModel : Any> getAction(workflow: Workflow<TModel>, kind: ActionKind): WorkflowAction<TModel>?
    fun <TModel : Any> invokeAction(action: WorkflowAction<TModel>)
}