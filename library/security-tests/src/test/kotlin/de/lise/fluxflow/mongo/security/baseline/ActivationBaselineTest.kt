package de.lise.fluxflow.mongo.security.baseline

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.stateful.StepActivationException
import de.lise.fluxflow.api.versioning.DefaultCompatibilityTester
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.workflow.ModelListenerDefinition
import de.lise.fluxflow.api.workflow.WorkflowDefinition
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.action.WorkflowActionDefinition
import de.lise.fluxflow.engine.job.JobActivationService
import de.lise.fluxflow.engine.job.JobActivationException
import de.lise.fluxflow.engine.reflection.ClassLoaderProvider
import de.lise.fluxflow.engine.step.DefaultStepActivationService
import de.lise.fluxflow.engine.step.StepTypeResolverImpl
import de.lise.fluxflow.engine.workflow.WorkflowImpl
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.job.JobDefinitionBuilder
import de.lise.fluxflow.stereotyped.job.parameter.ParameterDefinitionBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionFunctionResolverImpl
import de.lise.fluxflow.stereotyped.step.automation.AutomationDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataListenerDefinitionBuilder
import de.lise.fluxflow.stereotyped.versioning.VersionBuilder
import de.lise.fluxflow.validation.noop.NoOpDataValidationBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType

class ActivationBaselineTest {
    @Test
    fun `R01 persisted unregistered step kind is rejected before initialization`() {
        MongoSecurityHarness().use { harness ->
            val workflow = workflow()
            val identifier = UUID.randomUUID().toString()
            val activation = stepActivationService(harness.loader)
            harness.template.insert(
                StepDocument(
                    identifier,
                    workflow.identifier.value,
                    LegitimateStep::class.java.name,
                    null,
                    emptyMap(),
                    emptyMap(),
                    Status.Active,
                )
            )
            val legitimateStep = activation.activateFromPersistence(
                workflow,
                requireNotNull(harness.template.findById(identifier, StepDocument::class.java)).toStepData(),
            )
            assertThat(legitimateStep.identifier.value).isEqualTo(identifier)
            assertThat(legitimateStep.definition.kind).isEqualTo(StepKind(LegitimateStep::class.java.name))
            assertThat(legitimateStep.status).isEqualTo(Status.Active)
            harness.tamper(StepDocument::class.java, identifier, "kind", WITNESS_NAME)
            assertThat(harness.loader.events).isEmpty()

            val result = runCatching {
                activation.activateFromPersistence(
                    workflow,
                    requireNotNull(harness.template.findById(identifier, StepDocument::class.java)).toStepData(),
                )
            }

            assertThat(result.isFailure).isTrue()
            val failure = requireNotNull(result.exceptionOrNull())
            assertThat(failure)
                .isExactlyInstanceOf(StepActivationException::class.java)
                .hasMessage("Unable to activate step #$identifier with kind '$WITNESS_NAME'")
                .hasCauseExactlyInstanceOf(UnknownTypeException::class.java)
            val rejection = failure.cause as UnknownTypeException
            assertThat(rejection.role).isEqualTo(TypeRole.STEP)
            assertThat(rejection.key).isEqualTo(WITNESS_NAME)
            assertThat(harness.loader.events)
                .describedAs("R01 must reject the persisted kind before initializing or constructing it")
                .isEmpty()
        }
    }

    @Test
    fun `R02 persisted unregistered job kind is rejected before initialization`() {
        MongoSecurityHarness().use { harness ->
            val workflow = workflow()
            val identifier = UUID.randomUUID().toString()
            val activation = jobActivationService(harness.loader)
            harness.template.insert(
                JobDocument(
                    identifier,
                    workflow.identifier.value,
                    LegitimateJob::class.java.name,
                    emptyMap(),
                    Instant.parse("2026-09-09T12:00:00Z"),
                    null,
                    JobStatus.Scheduled,
                )
            )
            val legitimateJob = activation.activate(
                workflow,
                requireNotNull(harness.template.findById(identifier, JobDocument::class.java)).toJobData(),
            )
            assertThat(legitimateJob.identifier.value).isEqualTo(identifier)
            assertThat(legitimateJob.definition.kind.value).isEqualTo(LegitimateJob::class.java.name)
            assertThat(legitimateJob.status).isEqualTo(JobStatus.Scheduled)
            assertThat(legitimateJob.scheduledTime).isEqualTo(Instant.parse("2026-09-09T12:00:00Z"))
            harness.tamper(JobDocument::class.java, identifier, "kind", WITNESS_NAME)
            assertThat(harness.loader.events).isEmpty()

            val result = runCatching {
                activation.activate(
                    workflow,
                    requireNotNull(harness.template.findById(identifier, JobDocument::class.java)).toJobData(),
                )
            }

            assertThat(result.isFailure).isTrue()
            val failure = requireNotNull(result.exceptionOrNull())
            assertThat(failure)
                .isExactlyInstanceOf(JobActivationException::class.java)
                .hasMessage("Unable to activate job #$identifier with kind '$WITNESS_NAME'")
                .hasCauseExactlyInstanceOf(UnknownTypeException::class.java)
            val rejection = failure.cause as UnknownTypeException
            assertThat(rejection.role).isEqualTo(TypeRole.JOB)
            assertThat(rejection.key).isEqualTo(WITNESS_NAME)
            assertThat(harness.loader.events)
                .describedAs("R02 must reject the persisted kind before initializing or constructing it")
                .isEmpty()
        }
    }

    private fun stepActivationService(classLoader: ClassLoader): DefaultStepActivationService {
        val continuationBuilder = ContinuationBuilder()
        val metadataBuilder = MetadataBuilder()
        val parameterResolver = PriorityParameterResolver(emptyList())
        return DefaultStepActivationService(
            NoDependencies,
            StepDefinitionBuilder(
                VersionBuilder(),
                ActionDefinitionBuilder(
                    continuationBuilder,
                    metadataBuilder,
                    ActionFunctionResolverImpl(parameterResolver),
                ),
                DataDefinitionBuilder(
                    DataListenerDefinitionBuilder(continuationBuilder, parameterResolver),
                    NoOpDataValidationBuilder(),
                    metadataBuilder,
                ),
                metadataBuilder,
                AutomationDefinitionBuilder(continuationBuilder, parameterResolver),
            ),
            StepTypeResolverImpl(
                classLoader,
                mapOf(StepKind(LegitimateStep::class.java.name) to LegitimateStep::class),
            ),
            VersionCompatibility.Unknown,
            DefaultCompatibilityTester(),
        )
    }

    private fun jobActivationService(classLoader: ClassLoader): JobActivationService {
        val kind = LegitimateJob::class.java.name
        val typeRegistry = TypeRegistry.create(
            classLoader,
            listOf(TypeManifestEntry(TypeRole.JOB, kind, kind, "security test")),
        )
        return JobActivationService(
            NoDependencies,
            JobDefinitionBuilder(
                ParameterDefinitionBuilder(),
                ContinuationBuilder(),
                PriorityParameterResolver(emptyList()),
                MetadataBuilder(),
                mutableMapOf(),
            ),
            ClassLoaderProvider { classLoader },
            typeRegistry,
        )
    }

    private fun workflow(): WorkflowImpl<ActivationModel> = WorkflowImpl(
        object : WorkflowDefinition<ActivationModel> {
            override val updateListeners = emptyList<ModelListenerDefinition<ActivationModel>>()
            override val metadata = emptyMap<String, Any>()
            override val actions = emptyList<WorkflowActionDefinition<ActivationModel>>()
        },
        WorkflowIdentifier(UUID.randomUUID().toString()),
        ActivationModel("legitimate activation"),
        emptyMap(),
    )
}

private object NoDependencies : IocProvider {
    override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? = null

    override fun provide(type: KType): Any? = null
}

class LegitimateStep {
    fun execute() = Unit
}

class LegitimateJob {
    fun execute() = Unit
}

private data class ActivationModel(val value: String)
