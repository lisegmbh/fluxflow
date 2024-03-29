package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.query.StepQuery
import de.lise.fluxflow.api.step.query.filter.StepDefinitionFilter
import de.lise.fluxflow.api.step.query.filter.StepFilter
import de.lise.fluxflow.api.step.query.filter.StepKindFilter
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.ActionService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.engine.IntegrationTestConfig
import de.lise.fluxflow.query.filter.Filter
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [
        TestingConfiguration::class,
        IntegrationTestConfig::class
    ]
)
class StepIT {
    @Autowired
    var workflowStarterService: WorkflowStarterService? = null
    @Autowired
    var stepService: StepService? = null
    @Autowired
    var actionService: ActionService? = null

    @Test
    fun `findSteps with workflow and query should only include steps from a given workflow`() {
        // Arrange
        val workflow1 = workflowStarterService!!.start(null, Continuation.step(TestStep()))
        val workflow2 = workflowStarterService!!.start(null, Continuation.step(TestStep()))

        // Act
        val stepsFromWorkflow1 = stepService!!.findSteps(workflow1, StepQuery.empty())
        val stepsFromWorkflow2 = stepService!!.findSteps(workflow2, StepQuery.empty())

        // Assert
        stepsFromWorkflow1.items.forEach {
            assertThat(it.workflow.identifier).isEqualTo(workflow1.identifier)
        }
        stepsFromWorkflow2.items.forEach {
            assertThat(it.workflow.identifier).isEqualTo(workflow2.identifier)
        }
    }

    @Test
    fun `steps should not be completed if the continuation is set to preserve the step status`() {
        // Arrange
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithActions()))
        val stepBeforeAction = stepService!!.findSteps(workflow).first()
        val action = (stepBeforeAction as StatefulStep).actions.first {
            it.definition.kind.value == TestStepWithActions::doSomethingAndPreserveStepStatus.name
        }

        // Act
        actionService!!.invokeAction(action)

        // Assert
        val stepAfterAction = stepService!!.findStep(workflow, stepBeforeAction.identifier)!!
        assertThat(stepAfterAction.status).isEqualTo(Status.Active)
    }

    @Test
    fun `steps should be completed if the continuation is set not to preserve the step status`() {
        // Arrange
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithActions()))
        val stepBeforeAction = stepService!!.findSteps(workflow).first()
        val action = (stepBeforeAction as StatefulStep).actions.first {
            it.definition.kind.value == TestStepWithActions::doSomethingAndCompleteStep.name
        }

        // Act
        actionService!!.invokeAction(action)

        // Assert
        val stepAfterAction = stepService!!.findStep(workflow, stepBeforeAction.identifier)!!
        assertThat(stepAfterAction.status).isEqualTo(Status.Completed)
    }

    @Test
    fun `returning multiple continuations should be properly scheduled`() {
        // Arrange
        val workflow = workflowStarterService!!.start(null, Continuation.step(TestStepWithMultiContinuation()))
        val stepWithMultiContinuation = stepService!!.findSteps(workflow).first()
        val action = (stepWithMultiContinuation as StatefulStep).actions.first {
            it.definition.kind.value == TestStepWithMultiContinuation::run.name
        }

        // Act
        actionService!!.invokeAction(action)

        // Assert
        val stepsAfterActions = stepService!!.findSteps(
            workflow,
            StepQuery.withFilter(StepFilter(
                id = null,
                definition = StepDefinitionFilter(
                    kind = StepKindFilter.eq(StepKind(TestStep::class.qualifiedName!!))
                ),
                status = Filter.eq(Status.Active),
                metadata = null
            ))
        ).items

        assertThat(stepsAfterActions).hasSize(2)
    }
    
    @Test
    fun `onCreated automation functions triggered by a multiple continuation should be executed together`() {
        // Arrange
        val step1 = TestStepWithConsolidatedOnCreatedExecution()
        val step2 = TestStepWithConsolidatedOnCreatedExecution()
        
        // Act
        workflowStarterService!!.start(
            Any(), 
            Continuation.multiple(
                Continuation.step(step1),
                Continuation.step(step2)
            )
        )
        
        // Assert
        assertThat(step1.steps).hasSize(2)
        assertThat(step2.steps).hasSize(2)
    }
    
    @Test
    fun `StepService's setMetadata function should update a step's metadata`() {
        // Arrange
        val stepDefinition = TestStepWithMetadata()
        val workflow = workflowStarterService!!.start(
            Any(),
            Continuation.step(stepDefinition)
        )
        val step = stepService!!.findSteps(workflow).first()
        
        // Act
        val addedMetadata = stepService!!.setMetadata(step, "fromCode", true)
        val replacedMetadata = stepService!!.setMetadata(addedMetadata, "testMetadata", "from-code")
        val removedMetadata = stepService!!.setMetadata(replacedMetadata, "testMetadata", null)
        
        // Assert
        assertThat(addedMetadata.metadata).containsExactlyInAnyOrderEntriesOf(mapOf(
            "fromCode" to true,
            "testMetadata" to "from-annotation"
        ))
        assertThat(replacedMetadata.metadata).containsExactlyInAnyOrderEntriesOf(mapOf(
            "fromCode" to true,
            "testMetadata" to "from-code"
        ))
        assertThat(removedMetadata.metadata).containsExactlyInAnyOrderEntriesOf(mapOf(
            "fromCode" to true
        ))
    }
    
    @Test
    fun `activating steps having a custom kind should succeed`() {
        // Arrange
        val stepDefinition = TestStepWithCustomKind()
        val workflow = workflowStarterService!!.start(
            Any(),
            Continuation.step(stepDefinition)
        )
        
        // Act
        val step = stepService!!.findSteps(workflow).first()
        
        // Assert
        assertThat(step).isNotNull
    }
}