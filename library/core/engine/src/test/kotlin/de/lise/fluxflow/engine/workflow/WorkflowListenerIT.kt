package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.api.workflow.WorkflowStarterService
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.springboot.testing.TestingConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestingConfiguration::class])
class WorkflowListenerIT {
    @Autowired
    var workflowStarterService: WorkflowStarterService? = null
    @Autowired
    var workflowQueryService: WorkflowQueryService? = null
    @Autowired
    var workflowUpdateService: WorkflowUpdateService? = null

    @Test
    fun `listeners should not be invoked if no selector matches`() {
        // Act
        val result = doModifications(
            WorkflowModelWithListener()
        ) {
            it.aThirdProp
        }

        // Assert
        assertThat(result.model.invokeOnSomeOrAnotherPropChangeRecord).isNull()
    }

    @Test
    fun `listeners should be invoked if at least on selector matches`() {
        // Act
        val resultFromSomeProp = doModifications(
            WorkflowModelWithListener()
        ) {
            it.someProp = "Hello"
        }
        val resultFromAnotherProp = doModifications(
            WorkflowModelWithListener()
        ) {
            it.anotherProp = true
        }

        // Assert
        assertThat(resultFromSomeProp.model.invokeOnSomeOrAnotherPropChangeRecord).isNotNull()
        assertThat(resultFromAnotherProp.model.invokeOnSomeOrAnotherPropChangeRecord).isNotNull()
    }

    @Test
    fun `listeners should not be invoked if decision selector returns false`() {
        val resultFromSomeProp = doModifications(
            WorkflowModelWithListener().apply { someProp = "a" }
        ) {
            it.someProp = "b"
        }

        // Assert
        assertThat(resultFromSomeProp.model.invokeOnStringLengthChangeRecord).isNull()
    }

    @Test
    fun `listeners should be invoked if decision selector returns true`() {
        val resultFromSomeProp = doModifications(
            WorkflowModelWithListener().apply { someProp = "a" }
        ) {
            it.someProp = "aa"
        }

        // Assert
        assertThat(resultFromSomeProp.model.invokeOnStringLengthChangeRecord).isNotNull()
    }

    @Test
    fun `listeners should be able to receive services from dependency injection`() {
        val result = doModifications(
            WorkflowModelWithListener()
        ) {
            it.aThirdProp = 815
        }

        // Assert
        val changeRecord = result.model.invokeOnAnyChangeRecord
        assertThat(changeRecord).isNotNull()
        assertThat(changeRecord!!.hadStepService).isTrue()
    }

    @Test
    fun `listeners should be able to receive the workflow api object`() {
        val result = doModifications(
            WorkflowModelWithListener()
        ) {
            it.aThirdProp = 815
        }

        // Assert
        val changeRecord = result.model.invokeOnAnyChangeRecord
        assertThat(changeRecord).isNotNull()
        assertThat(changeRecord!!.workflowIdentifier).isEqualTo(result.identifier)
    }

    @Test
    fun `listeners should be able to request the old and updated model`() {
        val result = doModifications(
            WorkflowModelWithListener()
        ) {
            it.aThirdProp = 815
        }

        // Assert
        val changeRecord = result.model.invokeOnAnyChangeRecord
        assertThat(changeRecord).isNotNull()
        assertThat(changeRecord!!.oldModel!!.aThirdProp).isEqualTo(42)
        assertThat(changeRecord.newModel!!.aThirdProp).isEqualTo(815)
    }

    @Test
    fun `changes applied by listeners should not cause other listeners to be invoked`() {
        val result = doModifications(
            WorkflowModelWithListener()
        ) {
            it.aThirdProp = -42
        }

        // Assert
        assertThat(result.model.modifyingListenerRecord).isNotNull()
        assertThat(result.model.modifiedSomePropRecord).isNull()
    }

    private fun doModifications(
        oldModel: WorkflowModelWithListener,
        customizer: (model: WorkflowModelWithListener) -> Unit
    ): Workflow<WorkflowModelWithListener> {
        val createdWorkflow = workflowStarterService!!.start(
            oldModel,
            Continuation.none()
        )
        val loadedWorkflow = workflowQueryService!!.get<WorkflowModelWithListener>(createdWorkflow.identifier)
        customizer(loadedWorkflow.model)
        workflowUpdateService?.saveChanges(loadedWorkflow)
        return workflowQueryService!!.get(loadedWorkflow.identifier)
    }
}


