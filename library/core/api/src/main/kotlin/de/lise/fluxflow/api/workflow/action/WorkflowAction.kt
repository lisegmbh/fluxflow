package de.lise.fluxflow.api.workflow.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowActionService

/**
 * Represents an activated and executable workflow action.
 */
interface WorkflowAction<TModel> {
    /**
     * The workflow that this action has been created for. 
     */
    val workflow: Workflow<TModel>

    /**
     * The definition that was used to activate this action.
     */
    val definition: WorkflowActionDefinition<TModel>

    /**
     * Executes this action.
     *
     * **Note:** Calling [execute] directly does not handle side effects
     * (such as persisting state changes or emitting events).
     * Use [WorkflowActionService.invokeAction] instead.
     *
     * @return A continuation that controls how the workflow should proceed.
     */
    fun execute(): Continuation<*>
}