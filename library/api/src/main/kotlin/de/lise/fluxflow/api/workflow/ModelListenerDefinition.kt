package de.lise.fluxflow.api.workflow

/**
 * A [ModelListenerDefinition] defines a listener,
 * that will be called whenever relevant parts of a workflow's model have changed.
 * @param TModel The model type.
 */
interface ModelListenerDefinition<TModel> {
    /**
     * Checks if the model has changed in a way, that is relevant to the listener defined by this definition.
     * @param oldValue The old model value.
     * @param newValue The new model value.
     * @return `true`, if there has been a relevant change from [oldValue] to [newValue]. Otherwise `false`.
     */
    fun hasRelevantChanges(oldValue: TModel, newValue: TModel): Boolean

    /**
     * Create a new instance of a listener, as defined by this instance.
     * @param workflow The associated workflow.
     * @return The created [ModelListener] instance.
     */
    fun create(workflow: Workflow<TModel>): ModelListener<TModel>
}