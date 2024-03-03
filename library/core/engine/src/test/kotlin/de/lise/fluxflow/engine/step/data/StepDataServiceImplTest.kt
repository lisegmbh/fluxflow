package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.step.InvalidStepStateException
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.step.stateful.data.ModifiableData
import de.lise.fluxflow.api.step.stateful.data.UnmodifiableDataException
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.step.StepServiceImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock

class StepDataServiceImplTest {


    private fun defaultService(
        allowInactiveModification: Boolean,
        eventService: EventService? = null,
        stepServiceImpl: StepServiceImpl? = null,
        workflowUpdateService: WorkflowUpdateService? = null
    ): StepDataServiceImpl {
        return StepDataServiceImpl(
            stepServiceImpl ?: mock<StepServiceImpl> {},
            workflowUpdateService ?: mock<WorkflowUpdateService>{},
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
        val data = mock<Data<String>> {
            on { step } doReturn stepMock
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
        val data = mock<Data<String>> {
            on { step } doReturn stepMock
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
        val stepMock = mock<Step> {
            on { status } doReturn Status.Canceled
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
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
        val stepMock = mock<Step> {
            on { status } doReturn Status.Active
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
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
        val stepServiceImpl = mock<StepServiceImpl>{}
        val eventService = mock<EventService> {}
        val dataService = defaultService(
            false,
            eventService = eventService,
            stepServiceImpl = stepServiceImpl
        )
        val stepMock = mock<Step> {
            on { status } doReturn Status.Active
        }
        val dataDefinition = mock<DataDefinition<String>> {
            on { updateListeners } doReturn emptyList()
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
}