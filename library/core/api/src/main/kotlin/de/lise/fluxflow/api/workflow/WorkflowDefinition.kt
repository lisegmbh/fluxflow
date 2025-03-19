package de.lise.fluxflow.api.workflow

/**
 * The [WorkflowDefinition] holds information on a certain type of workflows.
 * @param TModel The workflow model's type.
 */
interface WorkflowDefinition<TModel> {
    /**
     * A list of listeners to be informed, whenever there are changes to the step model.
     */
    val updateListeners: List<ModelListenerDefinition<TModel>>
    
    /**
     * A simple key-value map providing additional metadata about this workflow definition.
     */
    val metadata: Map<String, Any>
}