package de.lise.fluxflow.stereotyped.workflow.action

import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.action.WorkflowAction
import de.lise.fluxflow.api.workflow.action.WorkflowActionDefinition

class ReflectedWorkflowActionDefinition<TModel>(
    override val kind: ActionKind,
    override val metadata: Map<String, Any>,
    private val actionCaller: WorkflowActionCaller<TModel>
) : WorkflowActionDefinition<TModel> {
    override fun createAction(workflow: Workflow<TModel>): WorkflowAction<TModel> {
        return ReflectedWorkflowAction(
            workflow,
            this,
            actionCaller
        )
    }
}