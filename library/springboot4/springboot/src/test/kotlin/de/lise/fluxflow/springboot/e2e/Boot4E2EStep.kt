package de.lise.fluxflow.springboot.e2e

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.stereotyped.step.action.Action
import de.lise.fluxflow.stereotyped.step.automation.OnCreated

class Boot4E2EStep(
    private val workflowModel: Boot4E2EWorkflowModel,
) {
    @OnCreated
    fun markPersistedOnCreate() {
        workflowModel.persistedMarker = "after-create"
    }

    @Action
    fun recordActionInvoked(): Continuation<*> =
        Continuation.none().withStatusBehavior(StatusBehavior.Preserve)
}
