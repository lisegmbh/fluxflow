package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.RestoredStep
import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition
import de.lise.fluxflow.api.versioning.DefaultCompatibilityTester
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.versioning.Version
import de.lise.fluxflow.api.versioning.NoVersion
import de.lise.fluxflow.api.step.automation.AutomationDefinition
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.engine.step.definition.StepDefinitionService
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionData
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionFunctionResolverImpl
import de.lise.fluxflow.stereotyped.step.automation.AutomationDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataListenerDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.validation.ValidationBuilder
import de.lise.fluxflow.stereotyped.versioning.VersionBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

class RestoringStepConstructionFailureTest {
    @Test
    fun `registered constructor failure stays visible with restore disabled for general failures`() {
        val kind = ConstructorFailureStep::class.java.name
        val definition = StepDefinitionData(kind, "", emptyMap(), emptyList())
        val persistence = mock<StepDefinitionPersistence> {
            on { findForKindAndVersion(kind, "") } doReturn definition
        }
        val base = DefaultStepActivationService(
            mock<IocProvider>(),
            definitionBuilder(),
            StepTypeResolverImpl(javaClass.classLoader, mapOf(StepKind(kind) to ConstructorFailureStep::class)),
            VersionCompatibility.Unknown,
            DefaultCompatibilityTester(),
        )
        val service = RestoringStepActivationService(base, StepDefinitionService(persistence), false)
        val workflow = mock<Workflow<Any>> {
            on { identifier } doReturn WorkflowIdentifier("workflow-id")
            on { model } doReturn Any()
        }
        val data = StepData("broken-step", "workflow-id", kind, "", emptyMap(), Status.Active, emptyMap())

        val failure = catchThrowable { service.activateFromPersistence(workflow, data) }

        assertThat(failure)
            .isInstanceOf(StepActivationException::class.java)
            .hasMessage("Unable to activate step #broken-step with kind '$kind'")
        assertThat(generateSequence(failure) { it.cause }.toList())
            .anyMatch { it is IllegalStateException && it.message == "constructor unavailable" }
    }

    @ParameterizedTest(name = "{0}, handleAnyException={1}")
    @MethodSource("technicalFailures")
    fun `technical failures require explicit restoration opt in`(scenario: TechnicalFailure, restore: Boolean) {
        val type = when (scenario) {
            TechnicalFailure.CONSTRUCTOR -> ConstructorFailureStep::class
            TechnicalFailure.DEFINITION_CONSTRUCTOR -> ConstructorFailureDefinition::class
            TechnicalFailure.BUILDER -> GenericPropertyStep::class
            else -> DependencyStep::class
        }
        val provider = dependencies {
            when (scenario) {
                TechnicalFailure.IOC -> throw IllegalStateException("container unavailable")
                TechnicalFailure.LINKAGE -> throw NoClassDefFoundError("missing dependency")
                TechnicalFailure.INITIALIZER -> throw ExceptionInInitializerError("initializer unavailable")
                else -> null
            }
        }
        val (service, data) = restoring(type, restore, provider)

        if (restore) {
            val result = service.activateFromPersistence(workflow(), data)

            assertThat(result).isInstanceOf(RestoredStep::class.java)
            assertThat((result as StatefulStep).actions).isEmpty()
            assertThat(result.identifier.value).isEqualTo("broken-step")
        } else {
            val failure = catchThrowable { service.activateFromPersistence(workflow(), data) }

            assertThat(failure)
                .isExactlyInstanceOf(TechnicalStepActivationException::class.java)
                .isInstanceOf(StepActivationException::class.java)
                .hasMessage("Unable to activate step #broken-step with kind '${data.kind}'")
            val expectedCause = when (scenario) {
                TechnicalFailure.BUILDER -> ClassCastException::class.java
                TechnicalFailure.LINKAGE -> NoClassDefFoundError::class.java
                TechnicalFailure.INITIALIZER -> ExceptionInInitializerError::class.java
                else -> IllegalStateException::class.java
            }
            assertThat(generateSequence(failure) { it.cause }.toList())
                .anyMatch { expectedCause.isInstance(it) }
        }
    }

    @Test
    fun `unknown persisted kind still restores without executable actions`() {
        val (service, data) = restoring(HealthyStep::class, registered = false)

        val result = service.activateFromPersistence(workflow(), data)

        assertThat(result).isInstanceOf(RestoredStep::class.java)
        assertThat((result as StatefulStep).actions).isEmpty()
    }

    @Test
    fun `incompatible persisted version still restores without executable actions`() {
        val (service, data) = restoring(VersionedStep::class, version = "1")

        val result = service.activateFromPersistence(workflow(), data)

        assertThat(result).isInstanceOf(RestoredStep::class.java)
        assertThat((result as StatefulStep).actions).isEmpty()
    }

    @Test
    fun `missing restore definition preserves the activation failure`() {
        val (service, data) = restoring(HealthyStep::class, registered = false, definitionAvailable = false)

        val failure = catchThrowable { service.activateFromPersistence(workflow(), data) }

        assertThat(failure)
            .isExactlyInstanceOf(StepActivationException::class.java)
            .hasMessageContaining("there is no persisted record of its definition")
            .hasCauseExactlyInstanceOf(StepActivationException::class.java)
    }

    @Test
    fun `explicit step activation failure from the container remains restorable`() {
        val rejection = StepActivationException("step cannot be activated")
        val (service, data) = restoring(DependencyStep::class, provider = dependencies { throw rejection })

        val result = service.activateFromPersistence(workflow(), data)

        assertThat(result).isInstanceOf(RestoredStep::class.java)
        assertThat((result as StatefulStep).actions).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("fatalFailures")
    fun `fatal runtime failures escape even with general restoration enabled`(fatal: Error) {
        val (service, data) = restoring(DependencyStep::class, true, dependencies { throw fatal })

        val failure = catchThrowable { service.activateFromPersistence(workflow(), data) }

        assertThat(failure).isSameAs(fatal)
    }

    private fun restoring(
        type: KClass<*>,
        restore: Boolean = false,
        provider: IocProvider = dependencies { null },
        registered: Boolean = true,
        version: String = "",
        definitionAvailable: Boolean = true,
    ): Pair<RestoringStepActivationService, StepData> {
        val kind = type.java.name
        val definition = if (definitionAvailable) StepDefinitionData(kind, version, emptyMap(), emptyList()) else null
        val persistence = mock<StepDefinitionPersistence> {
            on { findForKindAndVersion(kind, version) } doReturn definition
        }
        val base = DefaultStepActivationService(
            provider,
            definitionBuilder(),
            StepTypeResolverImpl(javaClass.classLoader, if (registered) mapOf(StepKind(kind) to type) else emptyMap()),
            VersionCompatibility.Unknown,
            DefaultCompatibilityTester(),
        )
        return RestoringStepActivationService(base, StepDefinitionService(persistence), restore) to
            StepData("broken-step", "workflow-id", kind, version, emptyMap(), Status.Active, emptyMap())
    }

    private fun workflow(): Workflow<Any> = mock {
        on { identifier } doReturn WorkflowIdentifier("workflow-id")
        on { model } doReturn Any()
    }

    private fun dependencies(resolve: () -> Any?): IocProvider = object : IocProvider {
        override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? =
            type.java.cast(resolve())

        override fun provide(type: KType): Any? = resolve()
    }

    private fun definitionBuilder(): StepDefinitionBuilder {
        val continuation = ContinuationBuilder()
        val metadata = MetadataBuilder()
        val parameters = PriorityParameterResolver(emptyList())
        val validation = object : ValidationBuilder {
            override fun <TInstance : Any, TProp> buildValidations(
                dataKind: DataKind,
                instanceType: KClass<out TInstance>,
                prop: KProperty1<TInstance, TProp>,
            ): DataValidationDefinition? = null
        }
        return StepDefinitionBuilder(
            VersionBuilder(),
            ActionDefinitionBuilder(continuation, metadata, ActionFunctionResolverImpl(parameters)),
            DataDefinitionBuilder(DataListenerDefinitionBuilder(continuation, parameters), validation, metadata),
            metadata,
            AutomationDefinitionBuilder(continuation, parameters),
        )
    }

    class ConstructorFailureStep {
        init {
            throw IllegalStateException("constructor unavailable")
        }
    }

    class HealthyStep

    @de.lise.fluxflow.stereotyped.versioning.Version("2")
    class VersionedStep

    class Dependency

    class DependencyStep(@Suppress("UNUSED_PARAMETER") dependency: Dependency)

    class GenericPropertyStep<T> {
        val value: T get() = error("The builder must reject the unresolved type before activation")
    }

    class ConstructorFailureDefinition : StepDefinition {
        init {
            throw IllegalStateException("definition constructor unavailable")
        }

        override val kind: StepKind get() = StepKind("broken-definition")
        override val version: Version get() = NoVersion()
        override val metadata: Map<String, Any> get() = emptyMap()
        override val onCreatedAutomations: Set<AutomationDefinition> get() = emptySet()
        override val onCompletedAutomations: Set<AutomationDefinition> get() = emptySet()
    }

    enum class TechnicalFailure {
        CONSTRUCTOR, DEFINITION_CONSTRUCTOR, IOC, BUILDER, LINKAGE, INITIALIZER,
    }

    companion object {
        @JvmStatic
        fun technicalFailures(): List<Arguments> = TechnicalFailure.entries.flatMap { failure ->
            listOf(false, true).map { restore -> Arguments.of(failure, restore) }
        }

        @JvmStatic
        fun fatalFailures(): List<Error> = listOf(object : VirtualMachineError("fatal") {}, ThreadDeath())
    }
}
