package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.state.ChangeDetector
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.engine.event.workflow.WorkflowUpdatedEvent
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class WorkflowUpdateServiceImplTest {
    @Test
    fun `workflows that didn't change should not be persisted`() {
        // Arrange
        val testId = WorkflowIdentifier("test")
        val testModel = Any()
        val oldWorkflowData = WorkflowData(testId.value, testModel)
        val workflowPersistence = mock<WorkflowPersistence> {
            on { find(eq(testId)) } doReturn oldWorkflowData
        }

        val changeDetector = mock<ChangeDetector<WorkflowData>> {
            on { hasChanged(any(), any()) } doReturn false
        }
        val eventService = mock<EventService> {}

        val workflowUpdateService = WorkflowUpdateServiceImpl(
            workflowPersistence,
            changeDetector,
            eventService
        )

        val workflow = mock<Workflow<Any>> {
            on { id } doReturn testId
            on { model } doReturn testModel
        }

        // Act
        workflowUpdateService.saveChanges(workflow)

        // Assert
        verify(changeDetector, times(1)).hasChanged(any(), any())
        verify(workflowPersistence, never()).save(any())
        verify(eventService, never()).publish(any<WorkflowUpdatedEvent>())
    }

}