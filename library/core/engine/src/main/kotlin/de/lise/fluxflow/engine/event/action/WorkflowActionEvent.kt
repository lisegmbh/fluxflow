package de.lise.fluxflow.engine.event.action

import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.action.WorkflowAction

data class WorkflowActionEvent<TModel>(
    val action: WorkflowAction<TModel>
) : FlowEvent {
    override val workflow: Workflow<*>
        get() = action.workflow
    override val context: Any?
        get() = null
}