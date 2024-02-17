package de.lise.fluxflow.stereotyped.workflow

import de.lise.fluxflow.api.workflow.ModelListener
import de.lise.fluxflow.api.workflow.Workflow

class ReflectedModelListener<TInstance, TModel>(
    private val workflow: Workflow<TModel>,
    private val instance: TInstance,
    private val callback: ModelListenerCallback<TInstance, TModel>
) : ModelListener<TModel> {
    override fun onChange(oldModel: TModel, newModel: TModel) {
        callback.call(workflow, instance, oldModel, newModel)
    }
}