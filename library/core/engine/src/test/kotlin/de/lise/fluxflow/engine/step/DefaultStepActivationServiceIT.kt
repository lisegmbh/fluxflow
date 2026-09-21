package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.versioning.DefaultCompatibilityTester
import de.lise.fluxflow.api.versioning.NoVersion
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.stereotyped.Import
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.versioning.VersionBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class DefaultStepActivationServiceIT {
    @Test
    fun `activateFromPersistence should be able to activate steps with simple imported data definitions`() {
        val workflow = mock<Workflow<Any>> { }
        val activationService = createActivationService()
        val stepData = StepData(
            id = "step-id",
            workflowId = "workflow-id",
            kind = TestStepWithImportedData::class.java.canonicalName,
            version = null,
            data = mapOf(
                "someProperty" to "Hello World"
            ),
            status = Status.Active,
            metadata = emptyMap()
        )
        
        // Act
        val result = activationService.activateFromPersistence(
            workflow,
            stepData
        )

        // Assert
        assertThat(result).isNotNull()
    }

    @Test
    fun `activateFromPersistence should be able to activate steps with prefixed imported data definitions`() {
        val workflow = mock<Workflow<Any>> { }
        val activationService = createActivationService()
        val stepData = StepData(
            id = "step-id",
            workflowId = "workflow-id",
            kind = TestStepWithPrefixedImportedData::class.java.canonicalName,
            version = null,
            data = mapOf(
                "subSomeProperty" to "The answer is 42"
            ),
            status = Status.Active,
            metadata = emptyMap()
        )

        // Act
        val result = activationService.activateFromPersistence(
            workflow,
            stepData
        )

        // Assert
        val data = (result as? StatefulStep)?.data?.single {
            it.definition.kind == DataKind("subSomeProperty")
        }
        assertThat(data).isNotNull()
        assertThat(data!!.get()).isEqualTo("The answer is 42")
    }

    private fun createActivationService(): DefaultStepActivationService = DefaultStepActivationService(
        iocProvider,
        StepDefinitionBuilder(
            mockedVersionBuilder,
            ActionDefinitionBuilder(
                mockedContinuationBuilder,
                mock {},
                mock {}
            ),
            DataDefinitionBuilder(
                mock {},
                mock {},
                mock {}
            ),
            mock {},
            mock {},
            mutableMapOf()
        ),
        StepTypeResolverImpl(
            TestStepWithImportedData::class.java.classLoader,
            mapOf(
                StepKind(TestStepWithImportedData::class.java.name) to TestStepWithImportedData::class,
                StepKind(TestStepWithPrefixedImportedData::class.java.name) to TestStepWithPrefixedImportedData::class,
            ),
        ),
        VersionCompatibility.Unknown,
        DefaultCompatibilityTester()
    )

    private val mockedVersionBuilder = mock<VersionBuilder> {
        on { build(any()) } doReturn NoVersion()
    }
    private val mockedContinuationBuilder = mock<ContinuationBuilder> { }
    private val iocProvider = mock<IocProvider> {}
}

data class ImportableTestModel(
    var someProperty: String
)

data class TestStepWithImportedData(
    @Import
    val importedProperty: ImportableTestModel
)

data class TestStepWithPrefixedImportedData(
    @Import("sub")
    val importedProperty: ImportableTestModel
)
