package de.lise.fluxflow.engine.job

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.job.query.JobQueryable
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.engine.reflection.ClassLoaderProvider
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import de.lise.fluxflow.scheduling.SchedulingService
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.job.Job as JobAnnotation
import de.lise.fluxflow.stereotyped.job.JobDefinitionBuilder
import de.lise.fluxflow.stereotyped.job.parameter.ParameterDefinitionBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType

class JobServiceImplTest {
    @Test
    fun `schedule should reject an unregistered job kind before persisting or cancelling`() {
        val persistence = mock<JobPersistence> {
            on { randomId() } doReturn "job-id"
            on { create(any()) } doReturn persisted("job-id", UnregisteredJob::class.qualifiedName!!)
        }
        val scheduling = mock<SchedulingService>()
        val service = jobService(TypeRegistry.create(javaClass.classLoader, emptyList()), persistence, scheduling)
        val continuation = Continuation.job(
            Instant.parse("2026-09-10T00:00:00Z"),
            UnregisteredJob(),
            CancellationKey("replace-me"),
        )

        val failure = catchThrowable {
            service.schedule(workflow(), continuation)
        }

        val kind = UnregisteredJob::class.qualifiedName
        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to schedule job with kind '$kind'")
            .hasCauseExactlyInstanceOf(UnknownTypeException::class.java)
        assertThat(failure.cause).extracting("role", "key").containsExactly(TypeRole.JOB, kind)
        verifyNoInteractions(persistence)
        verifyNoInteractions(scheduling)
    }

    @Test
    fun `duplicate should reject an unregistered job kind before persisting or cancelling`() {
        val persistence = mock<JobPersistence> {
            on { randomId() } doReturn "job-id"
            on { create(any()) } doReturn persisted("job-id", UnregisteredJob::class.qualifiedName!!)
        }
        val scheduling = mock<SchedulingService>()
        val service = jobService(TypeRegistry.create(javaClass.classLoader, emptyList()), persistence, scheduling)
        val definition = JobDefinitionBuilder(
            ParameterDefinitionBuilder(),
            ContinuationBuilder(),
            PriorityParameterResolver(emptyList()),
            MetadataBuilder(),
            mutableMapOf(),
        ).build(UnregisteredJob())
        val job = mock<Job> {
            on { this.definition } doReturn definition
            on { scheduledTime } doReturn Instant.parse("2026-09-10T00:00:00Z")
            on { cancellationKey } doReturn CancellationKey("replace-me")
            on { status } doReturn JobStatus.Scheduled
        }

        val failure = catchThrowable {
            service.duplicateJob(workflow(), job)
        }

        val kind = UnregisteredJob::class.qualifiedName
        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to schedule job with kind '$kind'")
            .hasCauseExactlyInstanceOf(UnknownTypeException::class.java)
        assertThat(failure.cause).extracting("role", "key").containsExactly(TypeRole.JOB, kind)
        verifyNoInteractions(persistence)
        verifyNoInteractions(scheduling)
    }

    @Test
    fun `schedule should reject a job kind that is registered only for another role`() {
        val kind = UnregisteredJob::class.qualifiedName!!
        val registry = TypeRegistry.create(
            javaClass.classLoader,
            listOf(TypeManifestEntry(TypeRole.MODEL, kind, UnregisteredJob::class.java.name, "test")),
        )
        val persistence = mock<JobPersistence> {
            on { randomId() } doReturn "job-id"
            on { create(any()) } doReturn persisted("job-id", kind)
        }
        val scheduling = mock<SchedulingService>()
        val service = jobService(registry, persistence, scheduling)

        val failure = catchThrowable {
            service.schedule(
                workflow(),
                Continuation.job(
                    Instant.parse("2026-09-10T00:00:00Z"),
                    UnregisteredJob(),
                    CancellationKey("replace-me"),
                ),
            )
        }

        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to schedule job with kind '$kind'")
        assertThat(failure.cause).extracting("role", "key").containsExactly(TypeRole.JOB, kind)
        verifyNoInteractions(persistence)
        verifyNoInteractions(scheduling)
    }

    @Test
    fun `schedule should persist a job whose kind is a registered alias`() {
        val kind = "reminder"
        val registry = TypeRegistry.create(
            javaClass.classLoader,
            listOf(TypeManifestEntry(TypeRole.JOB, kind, AliasedJob::class.java.name, "test")),
        )
        val persistence = mock<JobPersistence> {
            on { randomId() } doReturn "job-id"
            on { create(any()) } doAnswer { invocation -> invocation.getArgument<JobData>(0) }
        }
        val scheduling = mock<SchedulingService>()
        val service = jobService(registry, persistence, scheduling)

        val job = service.schedule(
            workflow(),
            Continuation.job(
                Instant.parse("2026-09-10T00:00:00Z"),
                AliasedJob(),
                CancellationKey("replace-me"),
            ),
        )

        assertThat(job.definition.kind.value).isEqualTo(kind)
        verify(persistence).create(check { assertThat(it.kind).isEqualTo(kind) })
        verify(scheduling).schedule(any(), any())
    }

    private fun jobService(
        registry: TypeRegistry,
        persistence: JobPersistence,
        scheduling: SchedulingService,
    ): JobServiceImpl = JobServiceImpl(
        JobActivationService(
            EmptyIoc,
            JobDefinitionBuilder(
                ParameterDefinitionBuilder(),
                ContinuationBuilder(),
                PriorityParameterResolver(emptyList()),
                MetadataBuilder(),
                mutableMapOf(),
            ),
            ClassLoaderProvider { javaClass.classLoader },
            registry,
        ),
        persistence,
        scheduling,
        mock<WorkflowService>(),
        mock<QueryMapper<JobQueryable, JobData>>(),
    )

    private fun workflow(): Workflow<Any> = mock {
        on { identifier } doReturn WorkflowIdentifier("workflow")
        on { model } doReturn Any()
    }

    private fun persisted(id: String, kind: String) = JobData(
        id,
        "workflow",
        kind,
        emptyMap(),
        Instant.parse("2026-09-10T00:00:00Z"),
        "replace-me",
        JobStatus.Scheduled,
    )

    class UnregisteredJob {
        fun execute() = Unit
    }

    @JobAnnotation("reminder")
    class AliasedJob {
        fun execute() = Unit
    }

    private object EmptyIoc : IocProvider {
        override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? = null

        override fun provide(type: KType): Any? = null
    }
}
