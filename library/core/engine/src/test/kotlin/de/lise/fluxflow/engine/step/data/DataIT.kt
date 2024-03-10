package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.engine.IntegrationTestConfig
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
class DataIT {
    @Autowired
    var workflowService: WorkflowService? = null

    @Autowired
    var stepService: StepService? = null

    private fun createTestMetadata(
        dataTestStep: DataTestStep,
        dataKind: DataKind
    ): Map<String, Any> {
        val workflow = workflowService!!.start(Any(), Continuation.step(dataTestStep))
        val step = stepService!!.findSteps(workflow).first() as StatefulStep
        return step.data
            .first { it.definition.kind == dataKind }
            .definition
            .metadata
    }

    @Test
    fun `data should carry metadata from backing field`() {
        // Act
        val metadata = createTestMetadata(
            DataTestStep(),
            DataKind(DataTestStep::someDataWithFieldMetadata.name)
        )

        // Assert
        assertThat(metadata).containsExactly(
            *mapOf("key" to "field").entries.toTypedArray()
        )
    }

    @Test
    fun `data should carry metadata from property`() {
        // Act
        val metadata = createTestMetadata(
            DataTestStep(),
            DataKind(DataTestStep::someDataWithPropertyMetadata.name)
        )

        // Assert
        assertThat(metadata).containsExactly(
            *mapOf("key" to "property").entries.toTypedArray()
        )
    }

    @Test
    fun `data should carry metadata from getter`() {
        // Act
        val metadata = createTestMetadata(
            DataTestStep(),
            DataKind(DataTestStep::someDataWithGetterMetadata.name)
        )

        // Assert
        assertThat(metadata).containsExactly(
            *mapOf("key" to "getter").entries.toTypedArray()
        )
    }
}