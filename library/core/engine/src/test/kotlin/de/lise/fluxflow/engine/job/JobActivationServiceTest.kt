package de.lise.fluxflow.engine.job

import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.job.JobKind
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.JobDefinition
import de.lise.fluxflow.api.job.parameter.Parameter
import de.lise.fluxflow.api.job.parameter.ParameterDefinition
import de.lise.fluxflow.api.continuation.Continuation
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
import de.lise.fluxflow.stereotyped.job.JobConfigurationException
import de.lise.fluxflow.stereotyped.job.JobPayload
import de.lise.fluxflow.stereotyped.job.parameter.ParameterDefinitionBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KType

class JobActivationServiceTest {
    @Test
    fun `registered job definitions retain their own definition and job factory`() {
        val typeName = DirectJobDefinition::class.java.name
        val service = service(registry(entry(TypeRole.JOB, typeName, typeName)))

        val activated = service.activate(workflow(), jobData("direct-job", typeName))

        assertThat(activated.definition).isExactlyInstanceOf(DirectJobDefinition::class.java)
        assertThat(activated.definition.kind.value).isEqualTo("direct-definition")
        assertThat(activated.identifier.value).isEqualTo("direct-job")
        assertThat(activated.metadata).containsEntry("source", "direct factory")
    }

    @Test
    fun `missing constructor dependencies fail with the job identifier and kind`() {
        val typeName = DependencyJob::class.java.name
        val service = service(registry(entry(TypeRole.JOB, typeName, typeName)))

        val failure = catchThrowable { service.activate(workflow(), jobData("unresolved-job", typeName)) }

        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to activate job #unresolved-job with kind '$typeName'")
            .hasNoCause()
    }

    @Test
    fun `container failures retain the original cause and job context`() {
        val typeName = DependencyJob::class.java.name
        val cause = IllegalStateException("container unavailable")
        val service = service(registry(entry(TypeRole.JOB, typeName, typeName)), failingProvider(cause))

        val failure = catchThrowable { service.activate(workflow(), jobData("container-job", typeName)) }

        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to activate job #container-job with kind '$typeName'")
            .hasCause(cause)
    }

    @ParameterizedTest
    @MethodSource("fatalFailures")
    fun `fatal runtime failures are not normalized as job activation errors`(fatal: Error) {
        val typeName = DependencyJob::class.java.name
        val service = service(registry(entry(TypeRole.JOB, typeName, typeName)), failingProvider(fatal))

        val failure = catchThrowable { service.activate(workflow(), jobData("fatal-job", typeName)) }

        assertThat(failure).isSameAs(fatal)
    }

    @Test
    fun `M08 should wrap a linkage failure after constructing the registered job`() {
        val typeName = BuilderLinkageJob::class.java.name
        val missingType = BuilderLinkageFixture.MissingMethodParameter::class.java.name
        val parent = javaClass.classLoader
        val loader = object : ClassLoader(parent) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(getClassLoadingLock(name)) {
                if (name == missingType) throw ClassNotFoundException(name)
                val type = findLoadedClass(name) ?: if (name == typeName) {
                    val bytes = requireNotNull(parent.getResourceAsStream("${name.replace('.', '/')}.class"))
                        .use { it.readBytes() }
                    defineClass(name, bytes, 0, bytes.size)
                } else {
                    super.loadClass(name, false)
                }
                if (resolve) resolveClass(type)
                type
            }
        }
        BuilderLinkageFixture.constructions.set(0)
        val registry = TypeRegistry.create(loader, listOf(entry(TypeRole.JOB, typeName, typeName)))
        assertThat(BuilderLinkageFixture.constructions.get()).isZero()
        val service = JobActivationService(NoDependencies, jobDefinitionBuilder(), ClassLoaderProvider { loader }, registry)

        val failure = catchThrowable {
            service.activate(workflow(), jobData("linkage-definition-job", typeName))
        }

        assertThat(BuilderLinkageFixture.constructions.get())
            .describedAs("The constructor must precede the builder failure; caught %s", failure?.stackTraceToString())
            .isEqualTo(1)
        val causes = generateSequence(failure) { it.cause }.toList()
        assertThat(causes.flatMap { it.stackTrace.toList() })
            .anyMatch { it.className == JobDefinitionBuilder::class.java.name }
        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to activate job #linkage-definition-job with kind '$typeName'")
            .hasCauseExactlyInstanceOf(NoClassDefFoundError::class.java)
    }

    @Test
    fun `M08 should wrap a registered job definition configuration failure`() {
        val typeName = AmbiguousPayloadJob::class.java.name
        val service = service(registry(entry(TypeRole.JOB, typeName, typeName)))

        val failure = catchThrowable {
            service.activate(workflow(), jobData("invalid-definition-job", typeName))
        }

        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to activate job #invalid-definition-job with kind '$typeName'")
            .hasCauseExactlyInstanceOf(JobConfigurationException::class.java)
        assertThat(failure.cause).hasMessageContaining("Found multiple explicitly declared payload functions")
    }

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
    fun `M08 should wrap a registered job constructor failure`() {
        val typeName = ThrowingJob::class.java.name
        val service = service(registry(entry(TypeRole.JOB, typeName, typeName)))

        val failure = catchThrowable {
            service.activate(workflow(), jobData("throwing-job", typeName))
        }

        assertThat(failure)
            .isExactlyInstanceOf(JobActivationException::class.java)
            .hasMessage("Unable to activate job #throwing-job with kind '$typeName'")
        assertThat(generateSequence(failure) { it.cause }.toList())
            .anyMatch { it is IllegalStateException && it.message == "job constructor failed" }
    }

    @Test
    fun `legacy JVM constructors remain available`() {
        assertThat(JobActivationService::class.java.constructors.map { it.parameterCount })
            .contains(3, 4)
        assertThat(JobActivation::class.java.constructors.map { it.parameterCount })
            .contains(5, 6)
    }

    private fun service(registry: TypeRegistry, provider: IocProvider = NoDependencies): JobActivationService = JobActivationService(
        provider,
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

    private fun failingProvider(failure: Throwable): IocProvider = object : IocProvider {
        override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? = throw failure
        override fun provide(type: KType): Any? = throw failure
    }

    class UnavailableDependency

    class DependencyJob(@Suppress("UNUSED_PARAMETER") dependency: UnavailableDependency) {
        fun execute() = Unit
    }

    class DirectJobDefinition : JobDefinition {
        override val kind = JobKind("direct-definition")
        override val parameters: List<ParameterDefinition<*>> = emptyList()
        override val metadata: Map<String, Any> = emptyMap()

        override fun createJob(
            jobIdentifier: JobIdentifier,
            workflow: Workflow<*>,
            scheduledTime: Instant,
            cancellationKey: CancellationKey?,
            status: JobStatus,
        ): de.lise.fluxflow.api.job.Job {
            val definition = this
            return object : de.lise.fluxflow.api.job.Job {
                override val identifier = jobIdentifier
                override val definition = definition
                override val scheduledTime = scheduledTime
                override val cancellationKey = cancellationKey
                override val status = status
                override val workflow = workflow
                override val parameters: List<Parameter<*>> = emptyList()
                override val metadata = mapOf("source" to "direct factory")
                override fun execute(): Continuation<*> = Continuation.none()
            }
        }
    }

    class AllowedJob {
        fun execute() = Unit
    }

    class AmbiguousPayloadJob {
        @JobPayload
        fun first() = Unit

        @JobPayload
        fun second() = Unit
    }

    @Job("allowed-job")
    class AliasedJob {
        fun execute() = Unit
    }

    class ThrowingJob {
        init {
            throw IllegalStateException("job constructor failed")
        }

        fun execute() = Unit
    }

    companion object {
        @JvmStatic
        fun fatalFailures(): List<Error> = listOf(object : VirtualMachineError("fatal") {}, ThreadDeath())
    }
}

private object NoDependencies : IocProvider {
    override fun <TDependency : Any> provide(type: KClass<out TDependency>): TDependency? = null

    override fun provide(type: KType): Any? = null
}
