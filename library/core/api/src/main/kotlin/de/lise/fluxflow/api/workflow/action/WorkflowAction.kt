package de.lise.fluxflow.api.workflow.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.Workflow

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
     * @return a continuation that controls how the workflow should be continued. 
     */
    fun execute(): Continuation<*>
}