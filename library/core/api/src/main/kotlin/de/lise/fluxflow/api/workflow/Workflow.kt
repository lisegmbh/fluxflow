package de.lise.fluxflow.api.workflow

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
}
