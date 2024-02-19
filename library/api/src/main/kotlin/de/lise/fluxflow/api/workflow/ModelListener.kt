package de.lise.fluxflow.api.workflow

/**
 * A model listener is invoked whenever a workflow's model changes.
 */
interface ModelListener<in TModel> {
    /**
     * Is invoked when the model changed.
     * @param oldModel The old model value.
     * @param newModel The new model value.
     */
    fun onChange(oldModel: TModel, newModel: TModel)
}