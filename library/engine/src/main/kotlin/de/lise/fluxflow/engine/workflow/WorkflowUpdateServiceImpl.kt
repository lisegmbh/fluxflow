package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.state.ChangeDetector
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.engine.event.workflow.BeforeWorkflowUpdateEvent
import de.lise.fluxflow.engine.event.workflow.WorkflowUpdatedEvent
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.slf4j.LoggerFactory

class WorkflowUpdateServiceImpl(
    private val persistence: WorkflowPersistence,
    private val changeDetector: ChangeDetector<WorkflowData>,
    private val eventService: EventService,
) : WorkflowUpdateService {
    override fun <TWorkflowModel> saveChanges(workflow: Workflow<TWorkflowModel>): Workflow<TWorkflowModel> {
        eventService.publish(BeforeWorkflowUpdateEvent(workflow))

        val oldWorkflowData = persistence.find(workflow.id)!!
        val updatedWorkflowData = oldWorkflowData.withModel(workflow.model)
        if(!changeDetector.hasChanged(oldWorkflowData, updatedWorkflowData)) {
            Logger.debug("Skip persisting/updating workflow '{}' as it hasn't changed.", workflow.id.value)
            return workflow
        }
        
        val updatedWorkflow = persistence.save(updatedWorkflowData)

        eventService.publish(WorkflowUpdatedEvent(workflow))

        return WorkflowImpl(updatedWorkflow)
    }
    
    private companion object {
        private val Logger = LoggerFactory.getLogger(WorkflowStarterServiceImpl::class.java)!! 
    }
}