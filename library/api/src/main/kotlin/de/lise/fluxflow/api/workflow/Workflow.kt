package de.lise.fluxflow.api.workflow

/**
 * Represents a workflow.
 * 
 * @param TModel The type of the data model storing all relevant workflow information.
 */
interface Workflow<TModel> {
    /**
     * Returns this workflow's unique identifier. 
     */
    val id: WorkflowIdentifier

    /**
     * Returns the data associated with this workflow.
     */
    val model: TModel
}
