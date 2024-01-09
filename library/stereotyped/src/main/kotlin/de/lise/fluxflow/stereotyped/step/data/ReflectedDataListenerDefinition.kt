package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.DataListener
import de.lise.fluxflow.api.step.stateful.data.DataListenerDefinition

class ReflectedDataListenerDefinition<TInstance, TModel>(
    private val instance: TInstance,
    private val callback: DataListenerCallback<TInstance, TModel>
) : DataListenerDefinition<TModel> {
    override fun create(step: Step): DataListener<TModel> {
        return ReflectedDataListener(
            step,
            this,
            instance,
            callback
        )
    }
}