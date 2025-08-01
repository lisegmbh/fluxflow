package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.api.continuation.history.ContinuationHistoryService
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
    private val eventService: EventService,
    private val continuationHistoryService: ContinuationHistoryService,
) : WorkflowRemovalService {
    override fun delete(identifierToDelete: WorkflowIdentifier) {
        val workflowDataToDelete = persistence.find(identifierToDelete)
            ?: throw WorkflowNotFoundException(identifierToDelete)

        val workflowToDelete = activationService.activate<Any>(workflowDataToDelete)

        persistence.delete(identifierToDelete)
        removeRelatedElements(
            identifierToDelete,
            setOf(WorkflowObjectKind.Step, WorkflowObjectKind.Job)
        )

        eventService.publish(
            WorkflowDeletedEvent(
                workflowToDelete
            )
        )
    }

    /**
     * Deletes the workflow with the given identifier.
     * Depending on the removal scope, it also deletes all related steps and jobs.
     * In contrast to the [delete] function, it does not publish a [WorkflowDeletedEvent].
     * @param identifierToDelete the id of the workflow to delete
     * @param removalScope the scope of the removal (e.g. [WorkflowObjectKind.Step], [WorkflowObjectKind.Job])
     */
    internal fun removeSilently(identifierToDelete: WorkflowIdentifier, removalScope: Set<WorkflowObjectKind>) {
        persistence.delete(identifierToDelete)

        removeRelatedElements(identifierToDelete, removalScope)
    }

    private fun removeRelatedElements(
        identifierToDelete: WorkflowIdentifier,
        removalScope: Set<WorkflowObjectKind>,
    ) {
        if (WorkflowObjectKind.Step in removalScope) stepService.deleteAllForWorkflow(identifierToDelete)
        if (WorkflowObjectKind.Job in removalScope) jobService.deleteAllForWorkflow(identifierToDelete)
        if (WorkflowObjectKind.ContinuationRecord in removalScope) continuationHistoryService.deleteAllForWorkflow(identifierToDelete)
    }
}