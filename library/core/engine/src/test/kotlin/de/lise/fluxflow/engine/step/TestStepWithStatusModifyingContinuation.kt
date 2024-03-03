package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.stereotyped.step.automation.OnCreated

class TestStepWithStatusModifyingContinuation {
    @OnCreated
    fun run(): Continuation<*> {
        return Continuation.none().withStatusBehavior(StatusBehavior.Complete)
    }
}