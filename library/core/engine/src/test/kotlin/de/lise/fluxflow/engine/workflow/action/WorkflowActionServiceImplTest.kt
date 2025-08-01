package de.lise.fluxflow.engine.workflow.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.api.workflow.action.WorkflowAction
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.event.action.WorkflowActionEvent
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class WorkflowActionServiceImplTest {
    @Test
    fun `invokeAction must publish the a WorkflowActionEvent`() {
        // Arrange
        val eventService = mock<EventService> { }
        val workflowUpdateService = mock<WorkflowUpdateService> { }
        val continuationService = mock<ContinuationService> { }

        val testWorkflow = mock<Workflow<Any>> { }
        val action = mock<WorkflowAction<Any>> {
            on { workflow } doReturn testWorkflow
        }

        val workflowActionService = WorkflowActionServiceImpl(
            eventService,
            workflowUpdateService,
            continuationService
        )

        // Act
        workflowActionService.invokeAction(action)

        // Assert
        val eventCaptor = argumentCaptor<FlowEvent>()
        verify(eventService).publish(eventCaptor.capture())
        assertThat(eventCaptor.singleValue.workflow).isSameAs(testWorkflow)
        assertThat(eventCaptor.singleValue).isInstanceOf(WorkflowActionEvent::class.java)
    }

    @Test
    fun `invokeAction must invoke the actions payload`() {
        // Arrange
        val eventService = mock<EventService> { }
        val workflowUpdateService = mock<WorkflowUpdateService> { }
        val continuationService = mock<ContinuationService> { }
        val action = mock<WorkflowAction<Any>> { }

        val workflowActionService = WorkflowActionServiceImpl(
            eventService,
            workflowUpdateService,
            continuationService
        )

        // Act
        workflowActionService.invokeAction(action)

        // Assert
        verify(action).execute()
    }

    @Test
    fun `invokeAction must persist changes to the workflow's model`() {
        // Arrange
        val eventService = mock<EventService> { }
        val workflowUpdateService = mock<WorkflowUpdateService> { }
        val continuationService = mock<ContinuationService> { }

        val testWorkflow = mock<Workflow<Any>> { }
        val action = mock<WorkflowAction<Any>> {
            on { workflow } doReturn testWorkflow
        }

        val workflowActionService = WorkflowActionServiceImpl(
            eventService,
            workflowUpdateService,
            continuationService
        )

        // Act
        workflowActionService.invokeAction(action)

        // Assert
        verify(workflowUpdateService).saveChanges(testWorkflow)
    }

    @Test
    fun `invokeAction must continue the workflow according to the returned continuation`() {
        // Arrange
        val eventService = mock<EventService> { }
        val workflowUpdateService = mock<WorkflowUpdateService> { }
        val continuationService = mock<ContinuationService> { }

        val continuation = mock<Continuation<*>> { }
        val testWorkflow = mock<Workflow<Any>> { }
        val action = mock<WorkflowAction<Any>> {
            on { workflow } doReturn testWorkflow
            on { execute() } doReturn continuation
        }

        val workflowActionService = WorkflowActionServiceImpl(
            eventService,
            workflowUpdateService,
            continuationService
        )

        // Act
        workflowActionService.invokeAction(action)

        // Assert
        verify(continuationService).execute(
            same(testWorkflow),
            same(null),
            same(continuation),
            any()
        )
    }
}