package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.step.InvalidStepStateException
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.*
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.event.step.data.StepDataEvent
import de.lise.fluxflow.engine.step.StepServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class StepDataServiceImplTest {
    private fun defaultService(
        allowInactiveModification: Boolean,
        eventService: EventService? = null,
        stepServiceImpl: StepServiceImpl? = null,
        workflowUpdateService: WorkflowUpdateService? = null
    ): StepDataServiceImpl {
        return StepDataServiceImpl(
            stepServiceImpl ?: mock<StepServiceImpl> {},
            workflowUpdateService ?: mock<WorkflowUpdateService> {},
            eventService ?: mock<EventService> {},
            allowInactiveModification,
            mock<ContinuationService> {}
        )
    }

    @Test
    fun `setValue should throw an exception if step is completed`() {
        // Arrange
        val dataService = defaultService(false)
        val stepMock = mock<Step> {
            on { status } doReturn Status.Completed
        }
        val stepDataDefinitionMock = mock<DataDefinition<String>> {
            on { modificationPolicy } doReturn ModificationPolicy.InheritSetting
        }
        val data = mock<Data<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn stepDataDefinitionMock
        }


        // Act & Assert
        assertThrows<InvalidStepStateException> {
            dataService.setValue(data, "")
        }
    }

    @Test
    fun `setValue should throw an exception if step is canceled`() {
        // Arrange
        val dataService = defaultService(false)
        val stepMock = mock<Step> {
            on { status } doReturn Status.Canceled
        }
        val stepDataDefinitionMock = mock<DataDefinition<String>> {
            on { modificationPolicy } doReturn ModificationPolicy.InheritSetting
        }
        val data = mock<Data<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn stepDataDefinitionMock
        }

        // Act & Assert
        assertThrows<InvalidStepStateException> {
            dataService.setValue(data, "")
        }
    }

    @Test
    fun `setValue should not throw an exception if inactive modification is enabled and step is canceled`() {
        // Arrange
        val dataService = defaultService(true)
        val testWorkflow = mock<Workflow<Any>> { }
        val stepMock = mock<Step> {
            on { workflow } doReturn testWorkflow
            on { status } doReturn Status.Canceled
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
            on { modificationPolicy } doReturn ModificationPolicy.InheritSetting
        }
        val data = mock<ModifiableData<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn dataDefinition
        }

        // Act & Assert
        assertDoesNotThrow {
            dataService.setValue(data, "")
        }
    }

    @Test
    fun `setValue should throw no exception if step is active`() {
        // Arrange
        val dataService = defaultService(false)
        val testWorkflow = mock<Workflow<Any>> { }
        val stepMock = mock<Step> {
            on { workflow } doReturn testWorkflow
            on { status } doReturn Status.Active
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
            on { modificationPolicy } doReturn ModificationPolicy.InheritSetting
        }
        val data = mock<ModifiableData<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn dataDefinition
        }

        // Act & Assert
        assertDoesNotThrow {
            dataService.setValue(data, "")
        }
    }


    @Test
    fun `setValue should throw an UnmodifiableDataException if the supplied data is not modifiable`() {
        // Arrange
        val dataService = defaultService(false)
        val stepMock = mock<Step> {
            on { status } doReturn Status.Active
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
            on { modificationPolicy } doReturn ModificationPolicy.InheritSetting
        }
        val data = mock<Data<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn dataDefinition
        }

        // Act & Assert
        assertThrows<UnmodifiableDataException> {
            dataService.setValue(data, "")
        }
    }

    @Test
    fun `setValue should persist changes before publishing the StepDateEvent`() {
        // Arrange
        val stepServiceImpl = mock<StepServiceImpl> {}
        val eventService = mock<EventService> {}
        val dataService = defaultService(
            false,
            eventService = eventService,
            stepServiceImpl = stepServiceImpl
        )
        val testWorkflow = mock<Workflow<Any>> { }
        val stepMock = mock<Step> {
            on { workflow } doReturn testWorkflow
            on { status } doReturn Status.Active
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
            on { modificationPolicy } doReturn ModificationPolicy.InheritSetting
        }
        val data = mock<ModifiableData<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn dataDefinition
        }

        // Act
        dataService.setValue(data, "Test")

        // Assert
        val executionOrder = inOrder(
            stepServiceImpl,
            eventService
        )

        executionOrder.verify(stepServiceImpl).saveChanges(any<Step>())
        executionOrder.verify(eventService).publish(any())
    }

    @Test
    fun `setValue should persist changes when step data modification policy is set to allow`() {
        // Arrange
        val stepServiceImpl = mock<StepServiceImpl> {}
        val eventService = mock<EventService> {}
        val dataService = defaultService(
            false,
            eventService = eventService,
            stepServiceImpl = stepServiceImpl
        )
        val testWorkflow = mock<Workflow<Any>> { }
        val stepMock = mock<Step> {
            on { workflow } doReturn testWorkflow
            on { status } doReturn Status.Completed
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
            on { modificationPolicy } doReturn ModificationPolicy.AllowInactiveModification
        }
        val data = mock<ModifiableData<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn dataDefinition
        }

        // Act
        dataService.setValue(data, "Test")

        // Assert
        verify(stepServiceImpl, times(1)).saveChanges(any<Step>())
    }

    @Test
    fun `setValue should not persist changes when step data modification policy is set to prevent`() {
        // Arrange
        val stepServiceImpl = mock<StepServiceImpl> {}
        val eventService = mock<EventService> {}
        val dataService = defaultService(
            false,
            eventService = eventService,
            stepServiceImpl = stepServiceImpl
        )
        val stepMock = mock<Step> {
            on { status } doReturn Status.Completed
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
            on { modificationPolicy } doReturn ModificationPolicy.PreventInactiveModification
        }
        val data = mock<ModifiableData<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn dataDefinition
        }

        // Act & Assert
        assertThrows<InvalidStepStateException> {
            dataService.setValue(data, "Test")
        }

        verify(stepServiceImpl, never()).saveChanges(any<Step>())
    }

    @Test
    fun `setValue should publish a StepDataEvent containing the provided context information`() {
        // Arrange
        val stepServiceImpl = mock<StepServiceImpl> {}
        val eventService = mock<EventService> {}
        val eventCaptor = argumentCaptor<StepDataEvent>()
        val dataService = defaultService(
            false,
            eventService = eventService,
            stepServiceImpl = stepServiceImpl
        )
        val testWorkflow = mock<Workflow<Any>> { }
        val stepMock = mock<Step> {
            on { workflow } doReturn testWorkflow
            on { status } doReturn Status.Active
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
            on { modificationPolicy } doReturn ModificationPolicy.InheritSetting
        }
        val data = mock<ModifiableData<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn dataDefinition
        }
        val testContext = "TestContext"

        // Act
        dataService.setValue(testContext, data, "Test")

        // Assert
        verify(eventService, times(1)).publish(eventCaptor.capture())
        val publishedEvent = eventCaptor.firstValue
        assertThat(publishedEvent.context).isEqualTo(testContext)
    }

    @Test
    fun `setValue should publish a StepDataEvent without context information if none has been provided`() {
        // Arrange
        val stepServiceImpl = mock<StepServiceImpl> {}
        val eventService = mock<EventService> {}
        val eventCaptor = argumentCaptor<StepDataEvent>()
        val dataService = defaultService(
            false,
            eventService = eventService,
            stepServiceImpl = stepServiceImpl
        )
        val testWorkflow = mock<Workflow<Any>> { }
        val stepMock = mock<Step> {
            on { workflow } doReturn testWorkflow
            on { status } doReturn Status.Active
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
            on { modificationPolicy } doReturn ModificationPolicy.InheritSetting
        }
        val data = mock<ModifiableData<String>> {
            on { step } doReturn stepMock
            on { definition } doReturn dataDefinition
        }

        // Act
        dataService.setValue(data, "Test")

        // Assert
        verify(eventService, times(1)).publish(eventCaptor.capture())
        val publishedEvent = eventCaptor.firstValue
        assertThat(publishedEvent.context).isNull()
    }
}