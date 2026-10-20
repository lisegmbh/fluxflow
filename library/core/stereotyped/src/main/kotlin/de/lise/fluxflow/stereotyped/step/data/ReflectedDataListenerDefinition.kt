package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.DataListener
import de.lise.fluxflow.api.step.stateful.data.DataListenerDefinition
import de.lise.fluxflow.stereotyped.step.InstanceAccessor

class ReflectedDataListenerDefinition<TInstance, TModel>(
    private val instanceAccessor: InstanceAccessor<TInstance>,
    private val callback: DataListenerCallback<TInstance, TModel>,
) : DataListenerDefinition<TModel> {
    override fun create(step: Step): DataListener<TModel> {
        val instance = instanceAccessor.get(step)
        return ReflectedDataListener(
            step,
            this,
            instance,
            callback
        )
    }
}