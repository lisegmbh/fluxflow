package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.DataListener

class ReflectedDataListener<TInstance, TModel>(
    private val step: Step,
    override val definition: ReflectedDataListenerDefinition<TInstance, TModel>,
    private val instance: TInstance,
    private val callback: DataListenerCallback<TInstance, TModel>,
) : DataListener<TModel> {
    override fun onChange(oldValue: TModel, newValue: TModel): Continuation<*> {
        return callback.call(step, instance, oldValue, newValue)
    }
}


