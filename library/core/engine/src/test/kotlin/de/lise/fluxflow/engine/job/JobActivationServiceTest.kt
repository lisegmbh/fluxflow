package de.lise.fluxflow.engine.job

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.engine.reflection.ClassLoaderProvider
import de.lise.fluxflow.engine.security.ActivationResolverWitness
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

class JobActivationServiceTest {
    @Test
    fun `M05 M10 should activate registered job FQCNs and aliases`() {
        val fqcn = AllowedJob::class.qualifiedName!!
        val registry = registry(
            entry(TypeRole.JOB, fqcn, AllowedJob::class.java.name),
            entry(TypeRole.JOB, "allowed-job", AliasedJob::class.java.name),
        )
        val service = service(registry)
        val workflow = workflow()

        val fqcnJob = service.activate(workflow, jobData("fqcn-job", fqcn))
        val aliasedJob = service.activate(workflow, jobData("aliased-job", "allowed-job"))

        assertThat(fqcnJob.identifier.value).isEqualTo("fqcn-job")
        assertThat(fqcnJob.definition.kind.value).isEqualTo(fqcn)
        assertThat(aliasedJob.identifier.value).isEqualTo("aliased-job")
        assertThat(aliasedJob.definition.kind.value).isEqualTo("allowed-job")
    }

    @Test
    fun `M06 should reject a job registered only for another role without initializing it`() {
        val witnessName = ActivationResolverWitness.JobOtherRoleWitness::class.java.name
        clearWitness(witnessName)
        val registry = registry(
            entry(TypeRole.MODEL, witnessName, witnessName),
        )
        val service = service(registry)

        val failure = catchThrowable {
            service.activate(workflow(), jobData("unsafe-job", witnessName))
        }

        assertThat(failure).isExactlyInstanceOf(JobActivationException::class.java)
        assertThat(failure)
            .hasMessage("Unable to activate job #unsafe-job with kind '$witnessName'")
            .hasCauseExactlyInstanceOf(UnknownTypeException::class.java)
        assertThat(failure.cause).extracting("role", "key")
            .containsExactly(TypeRole.JOB, witnessName)
        assertWitnessUntouched(witnessName)
    }

    @Test
    fun `M06 legacy service constructor should reject an unregistered class without initializing it`() {
        val witnessName = ActivationResolverWitness.JobLegacyWitness::class.java.name
        clearWitness(witnessName)
        val service = JobActivationService(
            NoDependencies,
            jobDefinitionBuilder(),
            ClassLoaderProvider { javaClass.classLoader },
        )

        val failure = catchThrowable {
            service.activate(workflow(), jobData("unsafe-legacy-job", witnessName))
        }

        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasCauseExactlyInstanceOf(UnknownTypeException::class.java)
        assertWitnessUntouched(witnessName)
    }

    @Test
    fun `M09 should preserve legacy JVM constructors`() {
        assertThat(JobActivationService::class.java.constructors.map { it.parameterCount })
            .contains(3, 4)
        assertThat(JobActivation::class.java.constructors.map { it.parameterCount })
            .contains(5, 6)
    }

    private fun service(registry: TypeRegistry): JobActivationService = JobActivationService(
        NoDependencies,
        jobDefinitionBuilder(),
        ClassLoaderProvider { javaClass.classLoader },
        registry,
    )

    private fun jobDefinitionBuilder(): JobDefinitionBuilder = JobDefinitionBuilder(
        ParameterDefinitionBuilder(),
        ContinuationBuilder(),
        PriorityParameterResolver(emptyList()),
        MetadataBuilder(),
        mutableMapOf(),
    )

    private fun workflow(): Workflow<Any> = mock {
        on { model } doReturn Any()
        on { identifier } doReturn WorkflowIdentifier("workflow")
    }

    private fun jobData(id: String, kind: String) = JobData(
        id,
        "workflow",
        kind,
        emptyMap(),
        Instant.parse("2026-09-10T00:00:00Z"),
        null,
        JobStatus.Scheduled,
    )

    private fun registry(vararg entries: TypeManifestEntry): TypeRegistry =
        TypeRegistry.create(javaClass.classLoader, entries.toList())

    private fun entry(role: TypeRole, key: String, binaryClassName: String) =
        TypeManifestEntry(role, key, binaryClassName, "core-engine test")

    private fun clearWitness(name: String) {
        System.clearProperty("$name.initialized")
        System.clearProperty("$name.constructed")
    }

    private fun assertWitnessUntouched(name: String) {
        assertThat(System.getProperty("$name.initialized")).isNull()
        assertThat(System.getProperty("$name.constructed")).isNull()
    }

    class AllowedJob {
        fun execute() = Unit
    }

    @Job("allowed-job")
    class AliasedJob {
        fun execute() = Unit
    }
}

private object NoDependencies : IocProvider {
    override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? = null

    override fun provide(type: KType): Any? = null
}
