package de.lise.fluxflow.mongo.e2e

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.stereotyped.step.action.Action
import de.lise.fluxflow.stereotyped.step.automation.OnCreated

class Boot4MongoE2EStep(
    private val workflowModel: Boot4MongoE2EWorkflowModel,
) {
    @OnCreated
    fun markPersistedOnCreate() {
        workflowModel.persistedMarker = "mongo-persisted"
    }

    @Action
    fun recordActionInvoked(): Continuation<*> =
        Continuation.none().withStatusBehavior(StatusBehavior.Preserve)
}
