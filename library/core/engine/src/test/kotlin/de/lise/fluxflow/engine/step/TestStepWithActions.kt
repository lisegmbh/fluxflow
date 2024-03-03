package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.stereotyped.step.action.Action

class TestStepWithActions {
    @Action
    fun doSomethingAndCompleteStep(): Continuation<*> {
        return Continuation.none()
    }

    @Action
    fun doSomethingAndPreserveStepStatus(): Continuation<*> {
        return Continuation.none().withStatusBehavior(StatusBehavior.Preserve)
    }
}