package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.stereotyped.step.action.Action
import de.lise.fluxflow.stereotyped.step.action.ImplicitStatusBehavior
import de.lise.fluxflow.stereotyped.step.automation.Automated
import de.lise.fluxflow.stereotyped.step.automation.OnCompleted
import de.lise.fluxflow.stereotyped.step.automation.OnCreated
import de.lise.fluxflow.stereotyped.step.automation.Trigger

class TestStepWithAutomation {
    @OnCreated
    fun runOnCreated() {
        onStartedRunCount++
    }

    @Automated(Trigger.OnCreated)
    fun automatedOnCreated(): Continuation<*> {
        automatedOnCreatedRunCount++
        return Continuation.step(TestStep()).withStatusBehavior(StatusBehavior.Preserve)
    }

    @OnCompleted
    fun runOnCompleted() {
        onCompletedRunCount++
    }

    @Automated(Trigger.OnCompleted)
    fun automatedOnCompleted() {
        automatedOnCompletedRunCount++
    }

    @Automated(Trigger.OnCreated)
    @Automated(Trigger.OnCompleted)
    fun automatedCombined() {
        automatedCombinedOnCreatedAndCompletedRunCount++
    }

    @OnCreated
    @OnCompleted
    fun onCreatedOnCompleted() {
        onCreatedOnCompletedRunCount++
    }

    @Action(statusBehavior = ImplicitStatusBehavior.Complete)
    fun noop() {}

    @Action
    fun noopWithMultipleContinuation(): Continuation<*> {
        return Continuation.multiple(
            Continuation.none().withStatusBehavior(StatusBehavior.Complete),
            Continuation.none().withStatusBehavior(StatusBehavior.Complete)
        )
    }

    companion object {
        var onStartedRunCount = 0
        var automatedOnCreatedRunCount = 0
        var onCompletedRunCount = 0
        var automatedOnCompletedRunCount = 0
        var automatedCombinedOnCreatedAndCompletedRunCount = 0
        var onCreatedOnCompletedRunCount = 0
    }
}