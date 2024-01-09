package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.continuation.Continuation

class TestStepWithMultiContinuation {
    fun run(): Continuation<*> {
        return Continuation.multiple(
            Continuation.step(TestStep()),
            Continuation.step(TestStep()),
        )
    }
}