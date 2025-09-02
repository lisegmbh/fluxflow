package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.DataListener
import de.lise.fluxflow.api.step.stateful.data.DataListenerDefinition
import de.lise.fluxflow.stereotyped.step.bind

class ReflectedDataListenerDefinition<TInstance, TModel>(
    private val callback: DataListenerCallback<TInstance, TModel>
) : DataListenerDefinition<TModel> {
    override fun create(step: Step): DataListener<TModel> {
        val instance = step.bind<TInstance>()!!
        return ReflectedDataListener(
            step,
            this,
            instance,
            callback
        )
    }
}