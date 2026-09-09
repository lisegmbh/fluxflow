package de.lise.fluxflow.mongo.security.baseline

import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class WorkflowModelBaselineTest {
    @Test
    fun `R03 nested model type is materialized by the current workflow persistence`() {
        MongoSecurityHarness().use { harness ->
            val identifier = WorkflowIdentifier(UUID.randomUUID().toString())
            val model = BaselineModel("legitimate model")
            harness.workflows.create(model, identifier)
            assertThat(harness.workflows.find(identifier)?.model).isEqualTo(model)
            harness.tamper(WorkflowDocument::class.java, identifier.value, "model._class", WITNESS_NAME)
            assertThat(harness.loader.events).isEmpty()

            val read = runCatching { harness.workflows.find(identifier) }

            if (java.lang.Boolean.getBoolean("fluxflow.security.expectRejection")) {
                assertThat(harness.loader.events)
                    .describedAs("R03 must reject the persisted type before initializing or constructing it")
                    .isEmpty()
                assertThat(read.isFailure).isTrue()
            } else {
                assertThat(read.getOrThrow()?.model?.javaClass?.name).isEqualTo(WITNESS_NAME)
                assertThat(harness.loader.events).containsExactly("initialized", "constructed")
            }
        }
    }
}

data class BaselineModel(val value: String)

internal const val WITNESS_NAME =
    "de.lise.fluxflow.mongo.security.baseline.fixture.ActivationWitness"
