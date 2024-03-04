package de.lise.fluxflow.engine.workflow

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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class WorkflowQueryServiceImplTest {

    private fun mockWorkflowService(
        workflowPersistence: WorkflowPersistence = mock<WorkflowPersistence>(),
        eventService: EventService = mock<EventService>(),
        stepService: StepServiceImpl = mock<StepServiceImpl>(),
        jobService: JobServiceImpl = mock<JobServiceImpl>(),
        activationService: WorkflowActivationService = mock<WorkflowActivationService> {
            on { activate<Any?>(any()) } doReturn mock<Workflow<Any?>> {}
        }
    ): WorkflowQueryServiceImpl {
        return WorkflowQueryServiceImpl(
            workflowPersistence,
            eventService,
            stepService,
            jobService,
            activationService
        )
    }

    private fun workflowPersistenceMockForIdentifier(workflowIdentifier: WorkflowIdentifier): WorkflowPersistence =
        mock<WorkflowPersistence> {
            on { find(workflowIdentifier) } doReturn WorkflowData(workflowIdentifier.value, null)
        }

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
            assertThat(firstValue).isInstanceOf(WorkflowDeletedEvent::class.java)
        }
    }
}