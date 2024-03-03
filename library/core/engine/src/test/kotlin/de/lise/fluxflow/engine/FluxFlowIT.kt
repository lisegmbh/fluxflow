package de.lise.fluxflow.engine

import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestingConfiguration::class])
class FluxFlowIT {

    @Autowired
    var stepService: StepService? = null

    @Autowired
    var workflowService: WorkflowService? = null

    @Test
    fun `stepService can be injected`() {
        assertThat(stepService).isNotNull
    }

    @Test
    fun `workflowService can be injected`() {
        assertThat(workflowService).isNotNull
    }
}