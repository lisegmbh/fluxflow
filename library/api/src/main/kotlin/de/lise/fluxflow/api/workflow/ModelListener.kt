package de.lise.fluxflow.api.workflow

/**
 * A model listener is invoked whenever a workflow's model changes.
 */
interface ModelListener<in TModel> {
    /**
     * Is invoked when the model changed.
     * @param oldValue The old model value.
     * @param newValue The new model value.
     */
    fun onChange(oldValue: TModel, newValue: TModel)
}