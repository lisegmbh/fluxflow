package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowNotFoundException
import de.lise.fluxflow.api.workflow.WorkflowRemovalService
import de.lise.fluxflow.engine.event.workflow.WorkflowDeletedEvent
import de.lise.fluxflow.engine.job.JobServiceImpl
import de.lise.fluxflow.engine.step.StepServiceImpl
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence

class WorkflowRemovalServiceImpl(
    private val persistence: WorkflowPersistence,
    private val activationService: WorkflowActivationService,
    private val stepService: StepServiceImpl,
    private val jobService: JobServiceImpl,
    private val eventService: EventService
) : WorkflowRemovalService {
    override fun delete(identifierToDelete: WorkflowIdentifier) {
        val workflowDataToDelete = persistence.find(identifierToDelete)
            ?: throw WorkflowNotFoundException(identifierToDelete)

        val workflowToDelete = activationService.activate<Any?>(workflowDataToDelete)

        persistence.delete(identifierToDelete)
        stepService.deleteAllForWorkflow(identifierToDelete)
        jobService.deleteAllForWorkflow(identifierToDelete)

        eventService.publish(
            WorkflowDeletedEvent(
                workflowToDelete
            )
        )
    }

    /**
     * Deletes the workflow with the given identifier and all of its related steps.
     * In contrast to the [delete] function, it does not delete jobs nor does it publish a [WorkflowDeletedEvent].
     * @param identifierToDelete the id of the workflow to delete
     */
    internal fun removeSilently(identifierToDelete: WorkflowIdentifier) {
        persistence.delete(identifierToDelete)
        stepService.deleteAllForWorkflow(identifierToDelete)
    }
}