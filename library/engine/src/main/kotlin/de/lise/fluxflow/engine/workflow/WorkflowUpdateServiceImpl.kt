package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.engine.event.workflow.BeforeWorkflowUpdateEvent
import de.lise.fluxflow.engine.event.workflow.WorkflowUpdatedEvent
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence

class WorkflowUpdateServiceImpl(
    private val persistence: WorkflowPersistence,
    private val eventService: EventService,
) : WorkflowUpdateService {
    override fun <TWorkflowModel> saveChanges(workflow: Workflow<TWorkflowModel>): Workflow<TWorkflowModel> {
        eventService.publish(BeforeWorkflowUpdateEvent(workflow))

        val data = persistence.find(workflow.id)!!
        val updatedWorkflow = persistence.save(data.withModel(workflow.model))

        eventService.publish(WorkflowUpdatedEvent(workflow))

        return WorkflowImpl(updatedWorkflow)
    }
}