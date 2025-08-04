package de.lise.fluxflow.engine.step.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.InvalidStepStateException
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.workflow.Workflow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ActionServiceImplTest {
    private fun defaultService(): ActionServiceImpl {
        return ActionServiceImpl(
            mock { },
            mock { },
            mock { },
            mock {}
        )
    }

    @Test
    fun `invokeAction should throw an exception if step is completed`() {
        // Arrange
        val actionService = defaultService()
        val stepMock = mock<Step> {
            on { status } doReturn Status.Completed
        }
        val action = mock<Action> {
            on { step } doReturn stepMock
        }


        // Act & Assert
        assertThrows<InvalidStepStateException> { actionService.invokeAction(action) }
    }

    @Test
    fun `invokeAction should throw an exception if step is canceled`() {
        // Arrange
        val actionService = defaultService()
        val stepMock = mock<Step> {
            on { status } doReturn Status.Canceled
        }
        val action = mock<Action> {
            on { step } doReturn stepMock
        }

        // Act & Assert
        assertThrows<InvalidStepStateException> { actionService.invokeAction(action) }
    }

    @Test
    fun `invokeAction should throw no exception if step is active`() {
        // Arrange
        val testWorkflow = mock<Workflow<Any>> { }
        val actionService = defaultService()
        val stepMock = mock<Step> {
            on { workflow } doReturn testWorkflow
            on { status } doReturn Status.Active
            on { identifier } doReturn StepIdentifier("test")
        }
        val action = mock<Action> {
            on { step } doReturn stepMock
            on { execute() } doReturn Continuation.none()
        }

        // Act & Assert
        assertDoesNotThrow {
            actionService.invokeAction(action)
        }
    }
}