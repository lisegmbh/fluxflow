package de.lise.fluxflow.reflection.types

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TypeManifestTest {
    @Test
    fun `M02 should write declarations deterministically`() {
        val declarations = listOf(
            TypeManifestEntry(TypeRole.VALUE, "currency", String::class.java.name, "test"),
            TypeManifestEntry(TypeRole.STEP, "review", ReviewStep::class.java.name, "test"),
            TypeManifestEntry(TypeRole.JOB, "notify", NotificationJob::class.java.name, "test"),
            TypeManifestEntry(TypeRole.MODEL, "order", OrderModel::class.java.name, "test"),
        )

        val forward = TypeManifest.write(declarations)
        val reverse = TypeManifest.write(declarations.reversed())

        assertThat(forward).isEqualTo(reverse)
        assertThat(forward).isEqualTo(
            """
            manifest.version=1
            step.review=de.lise.fluxflow.reflection.types.TypeManifestTest${'$'}ReviewStep
            job.notify=de.lise.fluxflow.reflection.types.TypeManifestTest${'$'}NotificationJob
            model.order=de.lise.fluxflow.reflection.types.TypeManifestTest${'$'}OrderModel
            value.currency=java.lang.String
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `M03 should reject malformed manifests with origin and line`() {
        val malformed = """
            manifest.version=1
            step.valid=java.lang.String
            unknown.invalid=java.lang.Integer
        """.trimIndent()

        assertThatThrownBy {
            TypeManifest.read("fixture.jar", malformed.reader())
        }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining("fixture.jar")
            .hasMessageContaining("line 3")
            .hasMessageContaining("unknown.invalid")
    }

    @Test
    fun `M03 should reject unsupported manifest versions`() {
        assertThatThrownBy {
            TypeManifest.read("future.jar", "manifest.version=2\n".reader())
        }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining("future.jar")
            .hasMessageContaining("version '2'")
    }

    @Test
    fun `M03 should reject duplicate entries inside one manifest`() {
        val duplicate = """
            manifest.version=1
            step.review=java.lang.String
            step.review=java.lang.Integer
        """.trimIndent()

        assertThatThrownBy {
            TypeManifest.read("duplicate.jar", duplicate.reader())
        }
            .isInstanceOf(TypeManifestException::class.java)
            .hasMessageContaining("duplicate.jar")
            .hasMessageContaining("line 3")
            .hasMessageContaining("step.review")
    }

    private class ReviewStep
    private class NotificationJob
    private class OrderModel
}
