package de.lise.fluxflow.springboot.e2e

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.ActionService
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [Boot4E2ETestConfiguration::class])
class FluxFlowBoot4InMemoryE2EIT {

    @Autowired
    lateinit var workflowStarterService: WorkflowStarterService

    @Autowired
    lateinit var workflowQueryService: WorkflowQueryService

    @Autowired
    lateinit var stepService: StepService

    @Autowired
    lateinit var actionService: ActionService

    @Test
    fun `should start workflow invoke action persist reload and continue on Spring Boot 4`() {
        val model = Boot4E2EWorkflowModel()
        val startedWorkflow = workflowStarterService.start(
            model,
            Continuation.step(Boot4E2EStep(model)),
        )
        val workflowId = startedWorkflow.identifier

        val activeStep = stepService.findSteps(startedWorkflow).first() as StatefulStep
        actionService.invokeAction(activeStep.actions.first())

        val reloadedWorkflow = workflowQueryService.get<Boot4E2EWorkflowModel>(workflowId)
        assertThat(reloadedWorkflow.model.persistedMarker).isEqualTo("after-create")

        val stepsAfterReload = stepService.findSteps(reloadedWorkflow)
        assertThat(stepsAfterReload).isNotEmpty()

        val reactivatedStep = stepsAfterReload.first() as StatefulStep
        stepService.complete(reactivatedStep)

        val finalWorkflow = workflowQueryService.get<Boot4E2EWorkflowModel>(workflowId)
        assertThat(finalWorkflow.identifier).isEqualTo(workflowId)
        assertThat(stepService.findSteps(finalWorkflow)).isNotEmpty()
    }
}
