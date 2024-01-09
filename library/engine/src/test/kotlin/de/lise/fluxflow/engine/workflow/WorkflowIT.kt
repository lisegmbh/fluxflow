package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.WorkflowNotFoundException
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestingConfiguration::class])
class WorkflowIT {

    @Autowired
    var workflowStarterService: WorkflowStarterService? = null
    
    @Autowired
    var workflowService: WorkflowService? = null

    @Test
    fun `deleted workflows should stop from being returned by search method`() {
        // Arrange
        val workflow = workflowStarterService!!.start(
            null,
            Continuation.none()
        )

        // Act
        workflowService!!.delete(workflow.id)

        // Assert
        assertTrue(workflowService!!.getAll().none { it.id == workflow.id })
    }

    @Test
    fun `get method should throw an exception when called on deleted workflows`() {
        val workflow = workflowStarterService!!.start(
            null,
            Continuation.none()
        )

        // Act
        workflowService!!.delete(workflow.id)

        //Assert
        assertThrows<WorkflowNotFoundException> { workflowService!!.get<Any>(workflow.id) }
    }

    @Test
    fun `find with model type should only return workflows that use that model type`() {
        // Arrange
        val stringModelWorkflow = workflowStarterService!!.start(
            "",
            Continuation.none()
        )
        val testModelWorkflow = workflowStarterService!!.start(
            WorkflowITTestModel(),
            Continuation.none()
        )

        // Act
        val testModelWorkflows = workflowService!!.getAll(WorkflowITTestModel::class)
        val stringModelWorkflows = workflowService!!.getAll(String::class)

        // Assert
        assertThat(testModelWorkflows.map { it.id })
            .contains(testModelWorkflow.id)
            .doesNotContain(stringModelWorkflow.id)
        assertThat(stringModelWorkflows.map { it.id })
            .containsExactly(stringModelWorkflow.id)
            .doesNotContain(testModelWorkflow.id)
    }
}

class WorkflowITTestModel {}