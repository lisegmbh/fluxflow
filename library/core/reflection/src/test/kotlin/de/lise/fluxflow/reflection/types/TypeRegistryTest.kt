package de.lise.fluxflow.reflection.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class TypeRegistryTest {
    @Test
    fun `M02 should merge identical registrations independent of input order`() {
        val first = entry(TypeRole.STEP, "review", ReviewStep::class, "a.jar")
        val second = entry(TypeRole.STEP, "review", ReviewStep::class, "b.jar")

        val forward = TypeRegistry.create(javaClass.classLoader, listOf(first, second))
        val reverse = TypeRegistry.create(javaClass.classLoader, listOf(second, first))

        assertThat(forward.entries).isEqualTo(reverse.entries)
        assertThat(forward.entries).hasSize(1)
        assertThat(forward.entries.single().origins).containsExactly("a.jar", "b.jar")
    }

    @Test
    fun `M02 should reject conflicting registrations with stable diagnostics`() {
        val first = entry(TypeRole.STEP, "review", ReviewStep::class, "z.jar")
        val second = entry(TypeRole.STEP, "review", OtherReviewStep::class, "a.jar")

        assertThatThrownBy {
            TypeRegistry.create(javaClass.classLoader, listOf(first, second))
        }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining("step")
            .hasMessageContaining("review")
            .hasMessageContaining(ReviewStep::class.java.name)
            .hasMessageContaining(OtherReviewStep::class.java.name)
            .hasMessageContaining("a.jar")
            .hasMessageContaining("z.jar")
    }

    @Test
    fun `M05 should resolve aliases and legacy keys only in their declared roles`() {
        val nestedKey = Nested::class.qualifiedName!!
        val registry = TypeRegistry.create(
            javaClass.classLoader,
            listOf(
                entry(TypeRole.STEP, "same-alias", ReviewStep::class, "steps"),
                entry(TypeRole.JOB, "same-alias", NotificationJob::class, "jobs"),
                entry(TypeRole.MODEL, "order", OrderModel::class, "models"),
                entry(TypeRole.VALUE, "currency", Currency::class, "values"),
                entry(TypeRole.STEP, nestedKey, Nested::class, "legacy"),
            )
        )

        assertThat(registry.resolve(TypeRole.STEP, "same-alias")).isEqualTo(ReviewStep::class)
        assertThat(registry.resolve(TypeRole.JOB, "same-alias")).isEqualTo(NotificationJob::class)
        assertThat(registry.resolve(TypeRole.MODEL, "order")).isEqualTo(OrderModel::class)
        assertThat(registry.resolve(TypeRole.VALUE, "currency")).isEqualTo(Currency::class)
        assertThat(registry.resolve(TypeRole.STEP, nestedKey)).isEqualTo(Nested::class)
        assertThat(registry.entries.single { it.key == nestedKey }.binaryClassName)
            .contains("TypeRegistryTest${'$'}Nested")

        assertThatThrownBy { registry.resolve(TypeRole.MODEL, "same-alias") }
            .isInstanceOf(UnknownTypeException::class.java)
        assertThatThrownBy { registry.resolve(TypeRole.STEP, "Same-Alias") }
            .isInstanceOf(UnknownTypeException::class.java)
        assertThatThrownBy { registry.resolve(TypeRole.STEP, Nested::class.java.name) }
            .isInstanceOf(UnknownTypeException::class.java)
    }

    @Test
    fun `M04 should resolve manifest classes without initializing them`() {
        val property = ManifestInitializationWitness::class.java.name + ".initialized"
        System.clearProperty(property)

        val registry = TypeRegistry.create(
            javaClass.classLoader,
            listOf(
                TypeManifestEntry(
                    TypeRole.VALUE,
                    "witness",
                    ManifestInitializationWitness::class.java.name,
                    "witness.jar",
                )
            )
        )

        assertThat(registry.resolve(TypeRole.VALUE, "witness").java.name)
            .isEqualTo(ManifestInitializationWitness::class.java.name)
        assertThat(System.getProperty(property)).isNull()

        Class.forName(ManifestInitializationWitness::class.java.name, true, javaClass.classLoader)
        assertThat(System.getProperty(property)).isEqualTo("true")
    }

    @Test
    fun `M07 should isolate immutable registries and concurrent lookups`() {
        val source = mutableListOf(entry(TypeRole.MODEL, "model", OrderModel::class, "first"))
        val first = TypeRegistry.create(javaClass.classLoader, source)
        source.clear()
        source += entry(TypeRole.MODEL, "model", OtherOrderModel::class, "second")
        val second = TypeRegistry.create(javaClass.classLoader, source)
        val pool = Executors.newFixedThreadPool(8)

        try {
            val tasks = (1..200).map { index ->
                Callable {
                    if (index % 2 == 0) {
                        first.resolve(TypeRole.MODEL, "model")
                    } else {
                        runCatching { first.resolve(TypeRole.MODEL, "unknown-$index") }.exceptionOrNull()
                    }
                }
            }

            val results = pool.invokeAll(tasks).map { it.get() }

            assertThat(results.filterIsInstance<Class<*>>()).isEmpty()
            assertThat(results.filterIsInstance<kotlin.reflect.KClass<*>>()).allMatch { it == OrderModel::class }
            assertThat(results.filterIsInstance<UnknownTypeException>()).hasSize(100)
            assertThat(first.entries).hasSize(1)
            assertThat(first.resolve(TypeRole.MODEL, "model")).isEqualTo(OrderModel::class)
            assertThat(second.resolve(TypeRole.MODEL, "model")).isEqualTo(OtherOrderModel::class)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `M08 should fail atomically when a manifest class is missing`() {
        val valid = entry(TypeRole.STEP, "valid", ReviewStep::class, "valid.jar")
        val missing = TypeManifestEntry(TypeRole.JOB, "missing", "missing.DoesNotExist", "broken.jar")

        assertThatThrownBy {
            TypeRegistry.create(javaClass.classLoader, listOf(valid, missing))
        }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining("job")
            .hasMessageContaining("missing")
            .hasMessageContaining("missing.DoesNotExist")
            .hasMessageContaining("broken.jar")
            .hasRootCauseInstanceOf(ClassNotFoundException::class.java)
    }

    @Test
    fun `M08 should wrap linkage errors without publishing a registry`() {
        val loader = object : ClassLoader(javaClass.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == "broken.LinkedType") {
                    throw NoClassDefFoundError("broken.Dependency")
                }
                return super.loadClass(name, resolve)
            }
        }

        assertThatThrownBy {
            TypeRegistry.create(
                loader,
                listOf(TypeManifestEntry(TypeRole.VALUE, "broken", "broken.LinkedType", "broken.jar"))
            )
        }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining("broken.LinkedType")
            .hasRootCauseInstanceOf(NoClassDefFoundError::class.java)
    }

    private fun entry(
        role: TypeRole,
        key: String,
        type: kotlin.reflect.KClass<*>,
        origin: String,
    ): TypeManifestEntry = TypeManifestEntry(role, key, type.java.name, origin)

    private class ReviewStep
    private class OtherReviewStep
    private class NotificationJob
    private class OrderModel
    private class OtherOrderModel
    private class Currency
    private class Nested
}
