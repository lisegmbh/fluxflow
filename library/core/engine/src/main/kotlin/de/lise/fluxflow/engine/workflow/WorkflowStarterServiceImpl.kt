package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.event.workflow.WorkflowCreatedEvent
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence

class WorkflowStarterServiceImpl(
    private val persistence: WorkflowPersistence,
    private val activationService: WorkflowActivationService,
    private val continuationService: ContinuationService,
    private val eventService: EventService
) : WorkflowStarterService {
    fun <TWorkflowModel : Any> create(
        workflowModel: TWorkflowModel,
        forcedId: WorkflowIdentifier?
    ): Workflow<TWorkflowModel> {
        val data = persistence.create(workflowModel, forcedId)
        val workflow = activationService.activate<TWorkflowModel>(data)
        eventService.publish(WorkflowCreatedEvent(workflow))

        return workflow
    }

    override fun <TWorkflowModel : Any, TContinuation> start(
        workflowModel: TWorkflowModel,
        continuation: Continuation<TContinuation>,
        originatingObject: ReferredWorkflowObject<*>?,
        forcedId: WorkflowIdentifier?
    ): Workflow<TWorkflowModel> {
        val createdWorkflow = create(workflowModel, forcedId)
        continuationService.execute(createdWorkflow, originatingObject, continuation)
        return createdWorkflow
    }
}