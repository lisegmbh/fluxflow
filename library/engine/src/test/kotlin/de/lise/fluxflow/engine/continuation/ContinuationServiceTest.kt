package de.lise.fluxflow.engine.continuation

import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.engine.continuation.history.ContinuationHistoryServiceImpl
import de.lise.fluxflow.engine.step.StepCreationResult
import de.lise.fluxflow.engine.step.StepServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class ContinuationServiceTest {
    
    @Test
    fun `step continuation should be written to history`() {
        // Arrange
        val continuationHistoryServiceImpl = mock<ContinuationHistoryServiceImpl> {}
        val createdStepIdentifier = StepIdentifier("test")
        val createdStep = mock<Step> {
            on { identifier } doReturn createdStepIdentifier
        }
        val stepCreationResult = StepCreationResult(createdStep, emptySet())
        val stepService = mock<StepServiceImpl> {
            on { create(any(), anyOrNull()) } doReturn(stepCreationResult)
        } 
        val continuationService = ContinuationService(
            stepService,
            mock {},
            mock {},
            continuationHistoryServiceImpl,
            mock {},
            mock {}
        )
        val workflowId = mock<WorkflowIdentifier> { }
        val workflow = mock<Workflow<Any>> { 
            on { id } doReturn workflowId
        }
        val continuation = Continuation.step(Any())
        
        // Act
        continuationService.execute(workflow, null, continuation)
        
        // Assert
        verify(continuationHistoryServiceImpl, times(1)).create(
            eq(workflowId),
            anyOrNull(),
            eq(continuation),
            eq(WorkflowObjectReference(
                WorkflowObjectKind.Step,
                createdStepIdentifier.value
            ))
        )
    }
}