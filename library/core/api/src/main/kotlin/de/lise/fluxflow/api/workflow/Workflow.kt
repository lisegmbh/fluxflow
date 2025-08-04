package de.lise.fluxflow.api.workflow

import de.lise.fluxflow.api.workflow.action.WorkflowAction

/**
 * Represents a workflow.
 * 
 * @param TModel The type of the data model storing all relevant workflow information.
 */
interface Workflow<TModel> {
    /**
     * Returns the definition describing this workflow.
     */
    val definition: WorkflowDefinition<TModel>
    
    /**
     * Returns this workflow's unique identifier. 
     */
    val identifier: WorkflowIdentifier

    /**
     * Returns the data associated with this workflow.
     */
    val model: TModel

    /**
     * Arbitrary metainformation associated with this workflow.
     */
    val metadata: Map<String, Any>

    /**
     * Holds the actions that can be executed in the scope of this workflow.
     */
    val actions: List<WorkflowAction<TModel>>
}
