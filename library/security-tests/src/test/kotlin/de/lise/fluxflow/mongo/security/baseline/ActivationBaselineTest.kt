package de.lise.fluxflow.mongo.security.baseline

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.versioning.DefaultCompatibilityTester
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.workflow.ModelListenerDefinition
import de.lise.fluxflow.api.workflow.WorkflowDefinition
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.action.WorkflowActionDefinition
import de.lise.fluxflow.engine.job.JobActivationService
import de.lise.fluxflow.engine.reflection.ClassLoaderProvider
import de.lise.fluxflow.engine.step.DefaultStepActivationService
import de.lise.fluxflow.engine.step.StepTypeResolverImpl
import de.lise.fluxflow.engine.workflow.WorkflowImpl
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
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
    fun `R01 persisted step kind initializes and constructs the current witness`() {
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
            activation.activateFromPersistence(
                workflow,
                requireNotNull(harness.template.findById(identifier, StepDocument::class.java)).toStepData(),
            )
            harness.tamper(StepDocument::class.java, identifier, "kind", WITNESS_NAME)
            assertThat(harness.loader.events).isEmpty()

            val result = runCatching {
                activation.activateFromPersistence(
                    workflow,
                    requireNotNull(harness.template.findById(identifier, StepDocument::class.java)).toStepData(),
                )
            }

            if (java.lang.Boolean.getBoolean("fluxflow.security.expectRejection")) {
                assertThat(harness.loader.events)
                    .describedAs("R01 must reject the persisted kind before initializing or constructing it")
                    .isEmpty()
                assertThat(result.isFailure).isTrue()
            } else {
                assertThat(result.isSuccess).isTrue()
                assertThat(harness.loader.events).containsExactly("initialized", "constructed")
            }
        }
    }

    @Test
    fun `R02 persisted job kind initializes and constructs the current witness`() {
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
            activation.activate(
                workflow,
                requireNotNull(harness.template.findById(identifier, JobDocument::class.java)).toJobData(),
            )
            harness.tamper(JobDocument::class.java, identifier, "kind", WITNESS_NAME)
            assertThat(harness.loader.events).isEmpty()

            val result = runCatching {
                activation.activate(
                    workflow,
                    requireNotNull(harness.template.findById(identifier, JobDocument::class.java)).toJobData(),
                )
            }

            if (java.lang.Boolean.getBoolean("fluxflow.security.expectRejection")) {
                assertThat(harness.loader.events)
                    .describedAs("R02 must reject the persisted kind before initializing or constructing it")
                    .isEmpty()
                assertThat(result.isFailure).isTrue()
            } else {
                assertThat(result.isSuccess).isTrue()
                assertThat(harness.loader.events).containsExactly("initialized", "constructed")
            }
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
            StepTypeResolverImpl(classLoader),
            VersionCompatibility.Unknown,
            DefaultCompatibilityTester(),
        )
    }

    private fun jobActivationService(classLoader: ClassLoader): JobActivationService = JobActivationService(
        NoDependencies,
        JobDefinitionBuilder(
            ParameterDefinitionBuilder(),
            ContinuationBuilder(),
            PriorityParameterResolver(emptyList()),
            MetadataBuilder(),
            mutableMapOf(),
        ),
        ClassLoaderProvider { classLoader },
    )

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
