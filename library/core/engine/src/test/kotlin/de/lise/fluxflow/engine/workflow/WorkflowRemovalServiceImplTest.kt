package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.api.continuation.history.ContinuationHistoryService
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowNotFoundException
import de.lise.fluxflow.engine.event.workflow.WorkflowDeletedEvent
import de.lise.fluxflow.engine.job.JobServiceImpl
import de.lise.fluxflow.engine.step.StepServiceImpl
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class WorkflowRemovalServiceImplTest {
    @Test
    fun `delete should throw an exception if the workflow to be deleted does not exist`() {
        // Arrange
        val persistenceMock = mock<WorkflowPersistence> {
            on { find(any()) } doReturn null
        }
        val workflowService = mockWorkflowService(
            workflowPersistence = persistenceMock,
        )

        // Act & Assert
        assertThrows<WorkflowNotFoundException> {
            workflowService.delete(
                WorkflowIdentifier("foo")
            )
        }
    }

    @Test
    fun `deleting a workflow should also delete all associated steps`() {
        // Arrange
        val workflowIdentifier = WorkflowIdentifier("workflowIdentifier")
        val workflowPersistenceMock = workflowPersistenceMockForIdentifier(workflowIdentifier)
        val stepServiceMock = mock<StepServiceImpl>()
        val workflowService = mockWorkflowService(
            workflowPersistence = workflowPersistenceMock,
            stepService = stepServiceMock,
        )

        // Act
        workflowService.delete(workflowIdentifier)

        // Assert
        verify(stepServiceMock).deleteAllForWorkflow(workflowIdentifier)
    }

    @Test
    fun `deleting a workflow should publish a WorkflowDeletedEvent`() {
        // Arrange
        val workflowIdentifier = WorkflowIdentifier("workflowIdentifier")
        val workflowPersistenceMock = workflowPersistenceMockForIdentifier(workflowIdentifier)
        val eventServiceMock = mock<EventService>()
        val workflowService = mockWorkflowService(
            workflowPersistence = workflowPersistenceMock,
            eventService = eventServiceMock,
        )

        // Act
        workflowService.delete(workflowIdentifier)

        // Assert
        argumentCaptor<FlowEvent>().apply {
            verify(eventServiceMock).publish(capture())
            Assertions.assertThat(firstValue).isInstanceOf(WorkflowDeletedEvent::class.java)
        }
    }

    @Test
    fun `deleting a workflow should also delete all associated jobs`() {
        // Arrange
        val workflowIdentifier = WorkflowIdentifier("workflowIdentifier")
        val workflowPersistenceMock = workflowPersistenceMockForIdentifier(workflowIdentifier)
        val jobServiceMock = mock<JobServiceImpl>()
        val workflowService = mockWorkflowService(
            workflowPersistence = workflowPersistenceMock,
            jobService = jobServiceMock
        )

        // Act
        workflowService.delete(workflowIdentifier)

        // Assert
        verify(jobServiceMock).deleteAllForWorkflow(workflowIdentifier)
    }

    @Test
    fun `removeSilently should delete the workflow without publishing a WorkflowDeletedEvent`() {
        // Arrange
        val workflowIdentifier = WorkflowIdentifier("workflowIdentifier")
        val workflowPersistenceMock = workflowPersistenceMockForIdentifier(workflowIdentifier)
        val eventServiceMock = mock<EventService>()
        val workflowService = mockWorkflowService(
            workflowPersistence = workflowPersistenceMock,
            eventService = eventServiceMock,
        )

        // Act
        workflowService.removeSilently(workflowIdentifier, emptySet())

        // Assert
        verify(eventServiceMock, never()).publish(any())
        verify(workflowPersistenceMock).delete(workflowIdentifier)
    }

    @Test
    fun `removeSilently should delete all associated elements based on the removal scope`() {
        // Arrange
        val workflowIdentifier = WorkflowIdentifier("workflowIdentifier")
        val workflowPersistenceMock = workflowPersistenceMockForIdentifier(workflowIdentifier)
        val stepServiceMock = mock<StepServiceImpl>()
        val continuationHistoryServiceMock = mock<ContinuationHistoryService>()
        val jobServiceMock = mock<JobServiceImpl>()
        val workflowService = mockWorkflowService(
            workflowPersistence = workflowPersistenceMock,
            stepService = stepServiceMock,
            jobService = jobServiceMock,
            continuationHistoryService = continuationHistoryServiceMock,
        )

        // Act
        workflowService.removeSilently(
            workflowIdentifier,
            WorkflowObjectKind.entries.toSet(),
        )

        // Assert
        verify(stepServiceMock).deleteAllForWorkflow(workflowIdentifier)
        verify(jobServiceMock).deleteAllForWorkflow(workflowIdentifier)
        verify(continuationHistoryServiceMock).deleteAllForWorkflow(workflowIdentifier)
    }

    private fun mockWorkflowService(
        workflowPersistence: WorkflowPersistence = mock<WorkflowPersistence>(),
        eventService: EventService = mock<EventService>(),
        stepService: StepServiceImpl = mock<StepServiceImpl>(),
        jobService: JobServiceImpl = mock<JobServiceImpl>(),
        activationService: WorkflowActivationService = mock<WorkflowActivationService> {
            on { activate<Any>(any()) } doReturn mock<Workflow<Any>> {}
        },
        continuationHistoryService: ContinuationHistoryService = mock<ContinuationHistoryService>(),
    ): WorkflowRemovalServiceImpl {
        return WorkflowRemovalServiceImpl(
            workflowPersistence,
            activationService,
            stepService,
            jobService,
            eventService,
            continuationHistoryService,
        )
    }

    private fun workflowPersistenceMockForIdentifier(workflowIdentifier: WorkflowIdentifier): WorkflowPersistence =
        mock<WorkflowPersistence> {
            on { find(workflowIdentifier) } doReturn WorkflowData(workflowIdentifier.value, null)
        }
}