package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.state.ChangeDetector
import de.lise.fluxflow.api.workflow.*
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
        val activationService = mock<WorkflowActivationService>{}

        val workflowUpdateService = WorkflowUpdateServiceImpl(
            workflowPersistence,
            changeDetector,
            eventService,
            activationService
        )

        val workflow = mock<Workflow<Any>> {
            on { identifier } doReturn testId
            on { model } doReturn testModel
        }

        // Act
        workflowUpdateService.saveChanges(workflow)

        // Assert
        verify(changeDetector, times(1)).hasChanged(any(), any())
        verify(workflowPersistence, never()).save(any())
        verify(eventService, never()).publish(any<WorkflowUpdatedEvent>())
    }

    @Test
    fun `model listeners should be created and invoked if there are relevant changes`() {
        // Arrange
        val testId = WorkflowIdentifier("test")
        val oldTestModel = Any()
        val oldWorkflowData = WorkflowData(testId.value, oldTestModel)
        val workflowPersistence = mock<WorkflowPersistence> {
            on { find(eq(testId)) } doReturn oldWorkflowData
        }

        val changeDetector = mock<ChangeDetector<WorkflowData>> {
            on { hasChanged(any(), any()) } doReturn true
        }
        val eventService = mock<EventService> {}
        val activationService = mock<WorkflowActivationService>{}

        val workflowUpdateService = WorkflowUpdateServiceImpl(
            workflowPersistence,
            changeDetector,
            eventService,
            activationService
        )

        val listener1 = mock<ModelListener<Any>> {}
        val listenerDefinition1 = mock<ModelListenerDefinition<Any>> {
            on { hasRelevantChanges(any(), any()) } doReturn true
            on { create(any()) } doReturn listener1
        }

        val listenerDefinition2 = mock<ModelListenerDefinition<Any>> {
            on { hasRelevantChanges(any(), any()) } doReturn false
        }

        val workflowDefinition = mock<WorkflowDefinition<Any>> {
            on { updateListeners } doReturn listOf(listenerDefinition1, listenerDefinition2)
        }

        val newTestModel = Any()
        val workflow = mock<Workflow<Any>> {
            on { identifier } doReturn testId
            on { model } doReturn newTestModel
            on { definition } doReturn workflowDefinition
        }

        // Act
        workflowUpdateService.saveChanges(workflow)

        // Assert
        verify(listenerDefinition1, times(1)).hasRelevantChanges(oldTestModel, newTestModel)
        verify(listenerDefinition1, times(1)).create(workflow)
        verify(listener1, times(1)).onChange(oldTestModel, newTestModel)

        verify(listenerDefinition2, times(1)).hasRelevantChanges(oldTestModel, newTestModel)
        verify(listenerDefinition2, never()).create(any())
    }
}