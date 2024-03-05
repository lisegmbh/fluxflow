package de.lise.fluxflow.engine.step.automation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.ActionService
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.engine.step.TestStep
import de.lise.fluxflow.engine.step.TestStepWithAutomation
import de.lise.fluxflow.engine.step.TestStepWithStatusModifyingContinuation
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestingConfiguration::class])
class StepAutomationIT {
    @Autowired
    var workflowStarterService: WorkflowStarterService? = null
    
    @Autowired
    var workflowQueryService: WorkflowQueryService? = null
    
    @Autowired
    var stepService: StepService? = null

    @Autowired
    var actionService: ActionService? = null
    
    @Test
    fun `onCreated automation functions should be run when a step is started`() {
        // Arrange
        TestStepWithAutomation.automatedOnCreatedRunCount = 0
        TestStepWithAutomation.onStartedRunCount = 0

        // Act
        workflowStarterService!!.start(null, Continuation.step(TestStepWithAutomation()))

        // Assert
        assertThat(TestStepWithAutomation.automatedOnCreatedRunCount).isEqualTo(1)
        assertThat(TestStepWithAutomation.onStartedRunCount).isEqualTo(1)
    }
    
    @Test
    fun `onCompleted automation functions should be run when a step is completed explicitly`() {
        // Arrange
        TestStepWithAutomation.automatedOnCompletedRunCount = 0
        TestStepWithAutomation.onCompletedRunCount = 0

        // Act & Assert
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithAutomation()))
        assertThat(TestStepWithAutomation.automatedOnCompletedRunCount).isZero()
        assertThat(TestStepWithAutomation.onCompletedRunCount).isZero()
        val step = stepService!!.findSteps(workflow).first()

        stepService!!.complete(step)
        assertThat(TestStepWithAutomation.automatedOnCompletedRunCount).isEqualTo(1)
        assertThat(TestStepWithAutomation.onCompletedRunCount).isEqualTo(1)
    }

    @Test
    fun `onCompleted automation functions should be run when a step is completed from executing an action`() {
        // Arrange
        TestStepWithAutomation.onCompletedRunCount = 0

        // Act
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithAutomation()))
        val stepAction = stepService!!.findSteps(workflow)
            .firstNotNullOf { it as? StatefulStep }.actions
            .first { it.definition.kind.value == "noop" }
        actionService!!.invokeAction(stepAction)


        // Assert
        assertThat(TestStepWithAutomation.onCompletedRunCount).isEqualTo(1)
    }

    @Test
    fun `onCompleted automation functions should only be run once when a step is completed by an action returning multiple continuations`() {
        // Arrange
        TestStepWithAutomation.onCompletedRunCount = 0

        // Act
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithAutomation()))
        val stepAction = stepService!!.findSteps(workflow)
            .firstNotNullOf { it as? StatefulStep }.actions
            .first { it.definition.kind.value == "noopWithMultipleContinuation" }
        actionService!!.invokeAction(stepAction)


        // Assert
        assertThat(TestStepWithAutomation.onCompletedRunCount).isEqualTo(1)
    }


    @Test
    fun `combined onCreated and onCompleted functions should be run during their associated phases`() {
        // Arrange
        TestStepWithAutomation.automatedCombinedOnCreatedAndCompletedRunCount = 0
        TestStepWithAutomation.onCreatedOnCompletedRunCount = 0

        // Act & Assert
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithAutomation()))
        assertThat(TestStepWithAutomation.automatedCombinedOnCreatedAndCompletedRunCount).isEqualTo(1)
        assertThat(TestStepWithAutomation.onCreatedOnCompletedRunCount).isEqualTo(1)
        val step = stepService!!.findSteps(workflow).first()

        stepService!!.complete(step)
        assertThat(TestStepWithAutomation.automatedCombinedOnCreatedAndCompletedRunCount).isEqualTo(2)
        assertThat(TestStepWithAutomation.onCreatedOnCompletedRunCount).isEqualTo(2)
    }

    @Test
    fun `onCreated automation functions should only be run once during creation and not during activation`() {
        // Arrange
        TestStepWithAutomation.automatedOnCreatedRunCount = 0
        TestStepWithAutomation.onStartedRunCount = 0

        // Act
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithAutomation()))
        stepService!!.findSteps(workflow)

        // Assert
        assertThat(TestStepWithAutomation.automatedOnCreatedRunCount).isEqualTo(1)
        assertThat(TestStepWithAutomation.onStartedRunCount).isEqualTo(1)
    }

    @Test
    fun `continuations returned from automation functions should be processed`() {
        // Act
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithAutomation()))

        // Assert
        val steps = stepService!!.findSteps(workflow)
        assertThat(
            steps.map { it.definition.kind.value }
        ).containsExactlyInAnyOrder(
            TestStepWithAutomation::class.qualifiedName,
            TestStep::class.qualifiedName
        )
    }

    @Test
    fun `status behavior should be respected when processing continuations returned from automated functions`() {
        // Arrange & Act
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithStatusModifyingContinuation()))

        // Assert
        val stepAfterAction = stepService!!.findSteps(workflow).single()
        assertThat(stepAfterAction.status).isEqualTo(Status.Completed)
    }
    
    @Test
    fun `workflow changes caused by onCreated should be persisted`() {
        // Arrange
        val model = TestWorkflowModelForModifyingAutomationFunctions(hasBeenChanged = false)

        // Act
        val startedWorkflow = workflowStarterService!!.start(
            model,
            Continuation.step(TestStepWithWorkflowDataModifyingAutomationFunction(model))    
        )
        
        // Assert
        val result = workflowQueryService!!.get<TestWorkflowModelForModifyingAutomationFunctions>(startedWorkflow.identifier)
        assertThat(result.model.hasBeenChanged).isTrue()
    }
    
    @Test
    fun `automation functions should be able to receive parameters from dependency injection`() {
        // Arrange
        val stepDefinition = TestWorkflowStepWithAutomationFunctionExpectingParameters()
        
        // Act
        workflowStarterService!!.start(null, Continuation.step(stepDefinition))

        // Assert
        assertThat(stepDefinition.receivedWorkflowStarterService).isSameAs(workflowStarterService)
    }
}