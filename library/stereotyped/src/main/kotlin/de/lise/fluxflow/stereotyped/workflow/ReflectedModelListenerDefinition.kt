package de.lise.fluxflow.stereotyped.workflow

import de.lise.fluxflow.api.workflow.ModelListener
import de.lise.fluxflow.api.workflow.ModelListenerDefinition
import de.lise.fluxflow.api.workflow.Workflow

class ReflectedModelListenerDefinition<TInstance, TModel>(
    private val instance: TInstance,
    private val callback: ModelListenerCallback<TInstance, TModel>
) : ModelListenerDefinition<TModel> {
    override fun hasRelevantChanges(oldValue: TModel, newValue: TModel): Boolean {
        return true // TODO: Add conditional changes
    }

    override fun create(workflow: Workflow<TModel>): ModelListener<TModel> {
        return ReflectedModelListener(
            workflow,
            instance,
            callback
        )
    }
}