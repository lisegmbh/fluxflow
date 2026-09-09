package de.lise.fluxflow.springboot.configuration

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.engine.job.JobActivationException
import de.lise.fluxflow.engine.reflection.ClassLoaderProvider
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.job.Job
import de.lise.fluxflow.stereotyped.job.JobDefinitionBuilder
import de.lise.fluxflow.stereotyped.job.parameter.ParameterDefinitionBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType

class BasicConfigurationActivationTest {
    @Test
    fun `O06 should pass the context registry to job activation`() {
        val registry = TypeRegistry.create(
            javaClass.classLoader,
            listOf(
                TypeManifestEntry(
                    TypeRole.JOB,
                    "context-job",
                    ContextJob::class.java.name,
                    "context registry test",
                ),
                TypeManifestEntry(
                    TypeRole.STEP,
                    "step-only",
                    ContextJob::class.java.name,
                    "context registry test",
                ),
            ),
        )
        val configuration = BasicConfiguration()
        BasicConfiguration::class.java.getDeclaredField("typeRegistry").apply {
            isAccessible = true
            set(configuration, registry)
        }
        val service = configuration.jobActivationService(
            WiringNoDependencies,
            jobDefinitionBuilder(),
            ClassLoaderProvider { javaClass.classLoader },
        )
        val workflow = mock<Workflow<Any>> {
            on { model } doReturn Any()
            on { identifier } doReturn WorkflowIdentifier("workflow")
        }

        val job = service.activate(workflow, jobData("allowed", "context-job"))
        val rejection = catchThrowable {
            service.activate(workflow, jobData("rejected", "step-only"))
        }

        assertThat(job.definition.kind.value).isEqualTo("context-job")
        assertThat(rejection)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasCauseExactlyInstanceOf(UnknownTypeException::class.java)
        assertThat(rejection.cause).extracting("role", "key")
            .containsExactly(TypeRole.JOB, "step-only")
    }

    private fun jobDefinitionBuilder(): JobDefinitionBuilder = JobDefinitionBuilder(
        ParameterDefinitionBuilder(),
        ContinuationBuilder(),
        PriorityParameterResolver(emptyList()),
        MetadataBuilder(),
        mutableMapOf(),
    )

    private fun jobData(id: String, kind: String) = JobData(
        id,
        "workflow",
        kind,
        emptyMap(),
        Instant.parse("2026-09-10T00:00:00Z"),
        null,
        JobStatus.Scheduled,
    )

    @Job("context-job")
    class ContextJob {
        fun execute() = Unit
    }
}

private object WiringNoDependencies : IocProvider {
    override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? = null

    override fun provide(type: KType): Any? = null
}
