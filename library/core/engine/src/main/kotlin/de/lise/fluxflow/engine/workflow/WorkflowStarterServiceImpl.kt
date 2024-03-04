package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.engine.continuation.ContinuationService

class WorkflowStarterServiceImpl(
    private val workflowService: WorkflowQueryServiceImpl,
    private val continuationService: ContinuationService
) : WorkflowStarterService {
    override fun <TWorkflowModel, TContinuation> start(
        workflowModel: TWorkflowModel,
        continuation: Continuation<TContinuation>,
        originatingObject: ReferredWorkflowObject<*>?,
        forcedId: WorkflowIdentifier?
    ): Workflow<TWorkflowModel> {
        val createdWorkflow = workflowService.create(workflowModel, forcedId)
        continuationService.execute(createdWorkflow, originatingObject, continuation)
        return createdWorkflow
    }
}