package de.lise.fluxflow.springboot.types

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.reflection.types.TypeRegistration
import de.lise.fluxflow.reflection.types.TypeManifest
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.springboot.activation.StepKindMapBuilder
import de.lise.fluxflow.springboot.types.fixtures.defaultapp.DefaultScannedJob
import de.lise.fluxflow.springboot.types.fixtures.defaultapp.DefaultScannedStep
import de.lise.fluxflow.springboot.types.fixtures.defaultapp.DefaultTypeApplication
import de.lise.fluxflow.springboot.types.fixtures.defaultapp.ExplicitUnannotatedModel
import de.lise.fluxflow.springboot.types.fixtures.explicitapp.ExplicitTypeApplication
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path

class FluxFlowTypeRegistryFactoryTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `M01 should discover annotated types from a default application package`() {
        context(DefaultTypeApplication::class.java).use { context ->
            val registry = FluxFlowTypeRegistryFactory(context, javaClass.classLoader).create()

            assertThat(registry.resolve(TypeRole.STEP, "scanned-step")).isEqualTo(DefaultScannedStep::class)
            assertThat(registry.resolve(TypeRole.JOB, "scanned-job")).isEqualTo(DefaultScannedJob::class)
            assertThat(StepKindMapBuilder(context, javaClass.classLoader, registry).build())
                .containsEntry(StepKind("scanned-step"), DefaultScannedStep::class)
        }
    }

    @Test
    fun `M01 should normalize explicit and overlapping scan roots`() {
        context(ExplicitTypeApplication::class.java).use { context ->
            val roots = SpringScanRootResolver(context).resolve()

            assertThat(roots).containsExactly(
                "de.lise.fluxflow.springboot.types.fixtures.defaultapp",
                "de.lise.fluxflow.springboot.types.fixtures.explicitapp",
            )
        }
    }

    @Test
    fun `M03 should merge an explicit unannotated type registration`() {
        val contributor = TypeRegistrationContributor {
            listOf(
                TypeRegistration(
                    TypeRole.MODEL,
                    "external-model",
                    ExplicitUnannotatedModel::class,
                )
            )
        }

        context(DefaultTypeApplication::class.java).use { context ->
            val registry = FluxFlowTypeRegistryFactory(
                context,
                javaClass.classLoader,
                listOf(contributor),
            ).create()

            assertThat(registry.resolve(TypeRole.MODEL, "external-model"))
                .isEqualTo(ExplicitUnannotatedModel::class)
        }
    }

    @Test
    fun `M04 should scan annotated types without initializing them`() {
        val witnessName =
            "de.lise.fluxflow.springboot.types.fixtures.defaultapp.ScanInitializationWitness"
        val property = "$witnessName.initialized"
        System.clearProperty(property)

        context(DefaultTypeApplication::class.java).use { context ->
            val registry = FluxFlowTypeRegistryFactory(context, javaClass.classLoader).create()

            assertThat(registry.resolve(TypeRole.STEP, "scan-witness").java.name)
                .isEqualTo(witnessName)
            assertThat(System.getProperty(property)).isNull()
        }

        Class.forName(witnessName, true, javaClass.classLoader)
        assertThat(System.getProperty(property)).isEqualTo("true")
    }

    @Test
    fun `M07 should keep explicit registrations local to their application context`() {
        val firstContributor = TypeRegistrationContributor {
            listOf(TypeRegistration(TypeRole.VALUE, "context-value", String::class))
        }
        val secondContributor = TypeRegistrationContributor {
            listOf(TypeRegistration(TypeRole.VALUE, "context-value", StringBuilder::class))
        }

        context(DefaultTypeApplication::class.java).use { firstContext ->
            context(DefaultTypeApplication::class.java).use { secondContext ->
                val first = FluxFlowTypeRegistryFactory(
                    firstContext,
                    javaClass.classLoader,
                    listOf(firstContributor),
                ).create()
                val second = FluxFlowTypeRegistryFactory(
                    secondContext,
                    javaClass.classLoader,
                    listOf(secondContributor),
                ).create()

                assertThat(first.resolve(TypeRole.VALUE, "context-value")).isEqualTo(String::class)
                assertThat(second.resolve(TypeRole.VALUE, "context-value")).isEqualTo(StringBuilder::class)
            }
        }
    }

    @Test
    fun `O06 should publish a complete registry before its first consumer`() {
        context(
            DefaultTypeApplication::class.java,
            TypeRegistryConfiguration::class.java,
            RegistryConsumerConfiguration::class.java,
        ).use { context ->
            val registry = context.getBean(RegistryConsumer::class.java).registry

            assertThat(registry.resolve(TypeRole.STEP, "scanned-step"))
                .isEqualTo(DefaultScannedStep::class)
        }
    }

    @Test
    fun `O06 should fail context refresh before a consumer sees an invalid manifest`() {
        val manifest = temporaryDirectory.resolve(TypeManifest.RESOURCE_PATH)
        Files.createDirectories(manifest.parent)
        Files.writeString(
            manifest,
            "manifest.version=1\nmodel.missing=missing.DoesNotExist\n",
        )
        val classLoader = URLClassLoader(
            arrayOf(temporaryDirectory.toUri().toURL()),
            javaClass.classLoader,
        )
        val context = AnnotationConfigApplicationContext()
        context.classLoader = classLoader
        context.register(
            DefaultTypeApplication::class.java,
            TypeRegistryConfiguration::class.java,
            RegistryConsumerConfiguration::class.java,
        )
        RegistryConsumer.created = false

        try {
            val failure = catchThrowable(context::refresh)

            assertThat(generateSequence(failure) { it.cause }.toList())
                .anyMatch { it is TypeManifestException }
            assertThat(RegistryConsumer.created).isFalse()
        } finally {
            context.close()
            classLoader.close()
        }
    }

    private fun context(vararg configurations: Class<*>): AnnotationConfigApplicationContext =
        AnnotationConfigApplicationContext(*configurations)

    @Configuration
    open class RegistryConsumerConfiguration {
        @Bean
        open fun registryConsumer(registry: TypeRegistry): RegistryConsumer = RegistryConsumer(registry)
    }

    class RegistryConsumer(
        val registry: TypeRegistry,
    ) {
        init {
            created = true
        }

        companion object {
            var created = false
        }
    }
}
