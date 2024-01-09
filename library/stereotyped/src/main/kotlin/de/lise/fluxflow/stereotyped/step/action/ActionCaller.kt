package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step

fun interface ActionCaller<TInstance> {
    fun call(
        step: Step,
        instance: TInstance
    ): Continuation<*>
}