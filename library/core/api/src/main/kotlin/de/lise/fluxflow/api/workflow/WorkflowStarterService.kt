package de.lise.fluxflow.api.workflow

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.continuation.Continuation

interface WorkflowStarterService {
    fun <TWorkflowModel, TContinuation> start(
        workflowModel: TWorkflowModel,
        continuation: Continuation<TContinuation>,
        originatingObject: ReferredWorkflowObject<*>? = null,
        forcedId: WorkflowIdentifier? = null
    ): Workflow<TWorkflowModel>
}