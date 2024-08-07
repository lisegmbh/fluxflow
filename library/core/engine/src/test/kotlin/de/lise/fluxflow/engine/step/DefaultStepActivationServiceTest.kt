package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.versioning.DefaultCompatibilityTester
import de.lise.fluxflow.api.versioning.NoVersion
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.continuation.ContinuationConverter
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.versioning.VersionBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.reflect.KClass

class DefaultStepActivationServiceTest {
    private val mockVersionBuilder = mock<VersionBuilder> {
        on { build(any()) } doReturn NoVersion()
    }

    @Test
    fun `activate should use the workflow's model as a constructor parameter, if the type matches`() {
        // Arrange
        val testParams = setup(
            TestService(),
            TestStepWithProcessModelConstructor::class,
            emptyMap(),
            emptyMap()
        )
        val workflow = mock<Workflow<TestModel>> {
            on { model }.doReturn(TestModel("foo"))
        }
        val activationService = testParams.first
        val testStep = testParams.second

        // Act
        val activatedStep =  assertDoesNotThrow {
            activationService.activate(workflow, testStep)
        }

        // Assert
        assertThat(activatedStep).isNotNull
    }

    @Test
    fun `activate should obtain constructor parameters from ioc container as a last resort effort`() {
        // Arrange
        val testParams = setup(
            TestService(),
            TestStepWithServiceConstructor::class,
            emptyMap(),
            emptyMap()
        )
        val workflow = mock<Workflow<Any>> {}
        val activationService = testParams.first
        val testStep = testParams.second


        // Act
        val activatedStep =  assertDoesNotThrow {
            activationService.activate(workflow, testStep)
        }

        // Assert
        assertThat(activatedStep).isNotNull
    }

    @Test
    fun `activate should obtain constructor parameters from step data`() {
        // Arrange
        val testParams = setup(
            TestService(),
            TestStepWithDataConstructor::class,
            mapOf(
                "testValue" to "some value"
            ),
            emptyMap()
        )
        val workflow = mock<Workflow<Any>> {}
        val activationService = testParams.first
        val testStepData = testParams.second

        // Act
        val activatedStep = assertDoesNotThrow {
            activationService.activate(workflow, testStepData)
        }

        // Assert
        assertThat(activatedStep).isNotNull
        val testValue = (activatedStep as? StatefulStep)
            ?.data
            ?.first { it.definition.kind.value == "testValue" }
            ?.get()
        assertThat(testValue).isEqualTo("some value")
    }


    private fun <TService, TStep : Any> setup(
        service: TService,
        stepType: KClass<TStep>,
        stepData: Map<String, Any?>,
        stepMetadata: Map<String, Any>
    ): Pair<StepActivationService, StepData> {
        val iocProvider = mock<IocProvider> {
            on { provide(any()) }.doReturn(service)
        }
        val testStepData = StepData(
            "step-id",
            "workflow-id",
            stepType.java.canonicalName,
            "",
            stepData,
            Status.Active,
            stepMetadata
        )
        val continuationConverter = mock<ContinuationConverter<Any?>> {}
        val continuationBuilder = mock<ContinuationBuilder> {
            on { createResultConverter<Any?>(any(), any(), any()) } doReturn continuationConverter 
        }
        
        val activationService = DefaultStepActivationService(
            iocProvider,
            StepDefinitionBuilder(
                mockVersionBuilder,
                ActionDefinitionBuilder(
                    continuationBuilder,
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
            StepTypeResolverImpl(stepType.java.classLoader),
            VersionCompatibility.Unknown,
            DefaultCompatibilityTester()
        )

        return Pair(activationService, testStepData)
    }
}

data class TestModel(val stringProperty: String)
class TestStepWithProcessModelConstructor(
    private val testModel: TestModel
)
class TestService {}
@Suppress("unused")
class TestStepWithServiceConstructor(
    private val testService: TestService
)
class TestStepWithDataConstructor(
    val testValue: String
)