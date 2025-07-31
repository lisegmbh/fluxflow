package de.lise.fluxflow.stereotyped.workflow.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.action.WorkflowAction
import de.lise.fluxflow.api.workflow.action.WorkflowActionDefinition

class ReflectedWorkflowAction<TModel>(
    override val workflow: Workflow<TModel>,
    override val definition: WorkflowActionDefinition<TModel>,
    private val actionCaller: WorkflowActionCaller<TModel>
): WorkflowAction<TModel> {
    override fun execute(): Continuation<*> {
        return actionCaller.call(workflow)
    }
}