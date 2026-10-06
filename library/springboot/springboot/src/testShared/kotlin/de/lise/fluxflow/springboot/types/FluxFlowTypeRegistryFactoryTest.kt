package de.lise.fluxflow.springboot.types

import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.StepConfigurationException
import de.lise.fluxflow.engine.reflection.ClassLoaderProvider
import de.lise.fluxflow.reflection.types.TypeRegistration
import de.lise.fluxflow.reflection.types.TypeManifest
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.reflection.types.UnknownTypeException
import de.lise.fluxflow.springboot.activation.StepKindMapBuilder
import de.lise.fluxflow.springboot.types.fixtures.defaultapp.DefaultScannedJob
import de.lise.fluxflow.springboot.types.fixtures.defaultapp.DefaultScannedStep
import de.lise.fluxflow.springboot.types.fixtures.defaultapp.DefaultTypeApplication
import de.lise.fluxflow.springboot.types.fixtures.defaultapp.ExplicitUnannotatedModel
import de.lise.fluxflow.springboot.types.fixtures.explicitapp.ExplicitTypeApplication
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.ApplicationContext
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.mockito.kotlin.mock
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

            assertThat(registry.resolve(TypeRole.STEP, witnessName).java.name)
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
    fun `M07 should scan with the application context class loader`() {
        context(DefaultTypeApplication::class.java).use { context ->
            URLClassLoader(emptyArray(), null).use { unrelatedClassLoader ->
                val thread = Thread.currentThread()
                val originalClassLoader = thread.contextClassLoader
                thread.contextClassLoader = unrelatedClassLoader
                try {
                    val registry = FluxFlowTypeRegistryFactory(
                        context,
                        javaClass.classLoader,
                    ).create()

                    assertThat(registry.resolve(TypeRole.STEP, "scanned-step"))
                        .isEqualTo(DefaultScannedStep::class)
                } finally {
                    thread.contextClassLoader = originalClassLoader
                }
            }
        }
    }

    @Test
    fun `M02 should identify contributor beans of the same implementation in conflicts`() {
        val first = FixedContributor(
            TypeRegistration(TypeRole.MODEL, "shared", String::class)
        )
        val second = FixedContributor(
            TypeRegistration(TypeRole.MODEL, "shared", StringBuilder::class)
        )

        context(DefaultTypeApplication::class.java).use { context ->
            assertThatThrownBy {
                FluxFlowTypeRegistryFactory(
                    context,
                    javaClass.classLoader,
                    linkedMapOf(
                        "firstTypes" to first,
                        "secondTypes" to second,
                    ),
                ).create()
            }
                .isInstanceOf(TypeManifestException::class.java)
                .hasMessageContaining("firstTypes")
                .hasMessageContaining("secondTypes")
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
        context.beanFactory.registerSingleton("classLoaderProvider", ClassLoaderProvider { classLoader })
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

    @Test
    fun `O06 should validate manifests when an application declares another registry bean`() {
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
        context.beanFactory.registerSingleton("classLoaderProvider", ClassLoaderProvider { classLoader })
        context.register(
            DefaultTypeApplication::class.java,
            TypeRegistryConfiguration::class.java,
            ForeignRegistryConfiguration::class.java,
        )

        try {
            val failure = catchThrowable(context::refresh)

            assertThat(generateSequence(failure) { it.cause }.toList())
                .anyMatch { it is TypeManifestException }
        } finally {
            context.close()
            classLoader.close()
        }
    }

    private fun context(vararg configurations: Class<*>): AnnotationConfigApplicationContext =
        AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("classLoaderProvider", ClassLoaderProvider { javaClass.classLoader })
            register(*configurations)
            refresh()
        }

    @Test
    fun `should load registry manifests through the configured class loader provider`() {
        val manifest = temporaryDirectory.resolve(TypeManifest.RESOURCE_PATH)
        Files.createDirectories(manifest.parent)
        Files.writeString(manifest, "manifest.version=1\nmodel.provider-only=java.lang.String\n")
        URLClassLoader(arrayOf(temporaryDirectory.toUri().toURL()), javaClass.classLoader).use { loader ->
            AnnotationConfigApplicationContext().use { context ->
                context.beanFactory.registerSingleton("classLoaderProvider", ClassLoaderProvider { loader })
                context.register(DefaultTypeApplication::class.java, TypeRegistryConfiguration::class.java)
                context.refresh()

                assertThat(context.getBean(TypeRegistry::class.java).resolve(TypeRole.MODEL, "provider-only"))
                    .isEqualTo(String::class)
            }
        }
    }

    @Test
    fun `should preserve the class identity supplied by a contributor`() {
        val original = ExplicitUnannotatedModel::class.java
        val bytes = original.getResourceAsStream("/${original.name.replace('.', '/')}.class")!!.use { it.readAllBytes() }
        val contributed = object : ClassLoader(javaClass.classLoader) {
            fun copy(): Class<*> = defineClass(original.name, bytes, 0, bytes.size)
        }.copy().kotlin
        val contributor = TypeRegistrationContributor {
            listOf(TypeRegistration(TypeRole.MODEL, "child-model", contributed))
        }

        context(DefaultTypeApplication::class.java).use { context ->
            val registry = FluxFlowTypeRegistryFactory(context, javaClass.classLoader, listOf(contributor)).create()

            assertThat(registry.resolve(TypeRole.MODEL, "child-model").java).isSameAs(contributed.java)
        }
    }

    @Test
    fun `should include contributor steps in the two argument builder`() {
        context(DefaultTypeApplication::class.java).use { context ->
            context.beanFactory.registerSingleton("plainSteps", FixedContributor(
                TypeRegistration(TypeRole.STEP, "plain-step", ExplicitUnannotatedModel::class)
            ))

            assertThat(StepKindMapBuilder(context, javaClass.classLoader).build())
                .containsEntry(StepKind("plain-step"), ExplicitUnannotatedModel::class)
        }
    }

    @Test
    fun `should reuse the context registry in the two argument builder`() {
        context(DefaultTypeApplication::class.java).use { context ->
            val registry = TypeRegistry.create(javaClass.classLoader, emptyList(), listOf(
                de.lise.fluxflow.reflection.types.TypeRegistryEntry(TypeRole.STEP, "registry-step",
                    ExplicitUnannotatedModel::class.java.name, ExplicitUnannotatedModel::class, listOf("context"))
            ))
            context.beanFactory.registerSingleton("existingRegistry", registry)

            assertThat(StepKindMapBuilder(context, javaClass.classLoader).build())
                .containsExactlyEntriesOf(mapOf(StepKind("registry-step") to ExplicitUnannotatedModel::class))
        }
    }

    @Test
    fun `should keep the step configuration exception for conflicting step kinds`() {
        context(DefaultTypeApplication::class.java).use { context ->
            context.beanFactory.registerSingleton("conflictingStep", FixedContributor(
                TypeRegistration(TypeRole.STEP, "scanned-step", String::class)
            ))

            assertThatThrownBy { StepKindMapBuilder(context, javaClass.classLoader).build() }
                .isInstanceOf(StepConfigurationException::class.java)
                .hasMessageContaining("scanned-step")
                .hasMessageContaining("conflictingStep")
        }
    }

    @Test
    fun `should discover steps through a non configurable application context`() {
        context(DefaultTypeApplication::class.java).use { context ->
            val plain = object : ApplicationContext by context {
                override fun getAutowireCapableBeanFactory(): AutowireCapableBeanFactory = mock()
            }

            assertThat(StepKindMapBuilder(plain, javaClass.classLoader).build())
                .containsEntry(StepKind("scanned-step"), DefaultScannedStep::class)
        }
    }

    @Test
    fun `should retain manifest exceptions for conflicts in other roles`() {
        context(DefaultTypeApplication::class.java).use { context ->
            context.beanFactory.registerSingleton("conflictingJob", FixedContributor(
                TypeRegistration(TypeRole.JOB, "scanned-job", String::class)
            ))

            assertThatThrownBy { StepKindMapBuilder(context, javaClass.classLoader).build() }
                .isInstanceOf(TypeManifestException::class.java)
                .hasMessageContaining("job")
        }
    }

    @Test
    fun `should skip annotation scans for packages covered by a manifest`() {
        val covered = DefaultScannedStep::class.java.packageName
        val loader = manifestLoader(
            """
            manifest.version=1
            manifest.covered-packages=$covered
            model.provider-only=java.lang.String
            """.trimIndent()
        )
        loader.use { classLoader ->
            context(DefaultTypeApplication::class.java).use { context ->
                context.beanFactory.registerSingleton(
                    "plainSteps",
                    FixedContributor(TypeRegistration(TypeRole.STEP, "plain-step", ExplicitUnannotatedModel::class)),
                )

                val registry = FluxFlowTypeRegistryFactory(context, classLoader).create()

                assertThat(registry.resolve(TypeRole.MODEL, "provider-only")).isEqualTo(String::class)
                assertThat(registry.resolve(TypeRole.STEP, "plain-step")).isEqualTo(ExplicitUnannotatedModel::class)
                assertThatThrownBy { registry.resolve(TypeRole.STEP, "scanned-step") }
                    .isInstanceOf(UnknownTypeException::class.java)
                assertThatThrownBy { registry.resolve(TypeRole.JOB, "scanned-job") }
                    .isInstanceOf(UnknownTypeException::class.java)
            }
        }
    }

    @Test
    fun `should still scan annotated types outside covered packages`() {
        val covered = ExplicitTypeApplication::class.java.packageName
        manifestLoader(
            """
            manifest.version=1
            manifest.covered-packages=$covered
            """.trimIndent()
        ).use { classLoader ->
            context(DefaultTypeApplication::class.java).use { context ->
                val registry = FluxFlowTypeRegistryFactory(context, classLoader).create()

                assertThat(registry.resolve(TypeRole.STEP, "scanned-step")).isEqualTo(DefaultScannedStep::class)
            }
        }
    }

    @Test
    fun `should scan annotated types when a manifest declares no coverage`() {
        manifestLoader(
            """
            manifest.version=1
            model.provider-only=java.lang.String
            """.trimIndent()
        ).use { classLoader ->
            context(DefaultTypeApplication::class.java).use { context ->
                val registry = FluxFlowTypeRegistryFactory(context, classLoader).create()

                assertThat(registry.resolve(TypeRole.STEP, "scanned-step")).isEqualTo(DefaultScannedStep::class)
                assertThat(registry.resolve(TypeRole.MODEL, "provider-only")).isEqualTo(String::class)
            }
        }
    }

    private fun manifestLoader(content: String): URLClassLoader {
        val manifest = temporaryDirectory.resolve(TypeManifest.RESOURCE_PATH)
        Files.createDirectories(manifest.parent)
        Files.writeString(manifest, "$content\n")
        return URLClassLoader(arrayOf(temporaryDirectory.toUri().toURL()), javaClass.classLoader)
    }

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

    private class FixedContributor(
        private val registration: TypeRegistration,
    ) : TypeRegistrationContributor {
        override fun registrations(): Iterable<TypeRegistration> = listOf(registration)
    }

    @Configuration(proxyBeanMethods = false)
    class ForeignRegistryConfiguration {
        @Bean
        fun foreignTypeRegistry(): TypeRegistry = TypeRegistry.create(
            javaClass.classLoader,
            emptyList(),
        )
    }
}
