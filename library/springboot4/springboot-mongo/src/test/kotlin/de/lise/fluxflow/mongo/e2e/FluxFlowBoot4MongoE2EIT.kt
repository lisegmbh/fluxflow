package de.lise.fluxflow.mongo.e2e

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.ActionService
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.mongo.Boot4MongoIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestConstructor

@Boot4MongoIntegrationTest
class FluxFlowBoot4MongoE2EIT {

    @Autowired
    lateinit var workflowStarterService: WorkflowStarterService

    @Autowired
    lateinit var workflowQueryService: WorkflowQueryService

    @Autowired
    lateinit var stepService: StepService

    @Autowired
    lateinit var actionService: ActionService

    @Test
    fun `should persist workflow in mongo reload from persistence and continue on Spring Boot 4`() {
        val model = Boot4MongoE2EWorkflowModel()
        val startedWorkflow = workflowStarterService.start(
            model,
            Continuation.step(Boot4MongoE2EStep(model)),
        )
        val workflowId = startedWorkflow.identifier

        val reloadedWorkflow = workflowQueryService.get<Boot4MongoE2EWorkflowModel>(workflowId)
        assertThat(reloadedWorkflow.model.persistedMarker).isEqualTo("mongo-persisted")

        val step = stepService.findSteps(reloadedWorkflow).first() as StatefulStep
        actionService.invokeAction(step.actions.first())
        stepService.complete(step)

        val afterContinue = workflowQueryService.get<Boot4MongoE2EWorkflowModel>(workflowId)
        assertThat(afterContinue.identifier).isEqualTo(workflowId)
        assertThat(stepService.findSteps(afterContinue)).isNotEmpty()
    }
}
