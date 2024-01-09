package de.lise.fluxflow.stereotyped.step.automation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step

fun interface AutomationFunctionCaller<TInstance> {
    fun call(
        step: Step,
        instance: TInstance
    ): Continuation<*>
}