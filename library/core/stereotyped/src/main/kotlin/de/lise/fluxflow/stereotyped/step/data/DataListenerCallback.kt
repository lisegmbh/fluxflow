package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step

fun interface DataListenerCallback<TInstance, TModel> {
    fun call(
        step: Step,
        instance: TInstance,
        oldValue: TModel,
        newValue: TModel
    ): Continuation<*>
}