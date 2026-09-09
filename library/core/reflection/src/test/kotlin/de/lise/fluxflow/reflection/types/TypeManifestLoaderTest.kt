package de.lise.fluxflow.reflection.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path

class TypeManifestLoaderTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `M02 should load and merge manifests from multiple artifacts deterministically`() {
        val firstArtifact = manifestDirectory(
            "first",
            """
            manifest.version=1
            step.review=${ReviewStep::class.java.name}
            """.trimIndent(),
        )
        val secondArtifact = manifestDirectory(
            "second",
            """
            manifest.version=1
            step.review=${ReviewStep::class.java.name}
            job.notification=${NotificationJob::class.java.name}
            """.trimIndent(),
        )

        val forward = load(firstArtifact, secondArtifact)
        val reverse = load(secondArtifact, firstArtifact)

        assertThat(forward.entries).isEqualTo(reverse.entries)
        assertThat(forward.entries).hasSize(2)
        assertThat(forward.resolve(TypeRole.STEP, "review")).isEqualTo(ReviewStep::class)
        assertThat(forward.resolve(TypeRole.JOB, "notification")).isEqualTo(NotificationJob::class)
        assertThat(forward.entries.single { it.role == TypeRole.STEP }.origins)
            .hasSize(2)
            .allMatch { it.endsWith("${TypeManifest.RESOURCE_PATH} at line 2") }
    }

    private fun manifestDirectory(name: String, content: String): Path {
        val directory = temporaryDirectory.resolve(name)
        val manifest = directory.resolve(TypeManifest.RESOURCE_PATH)
        Files.createDirectories(manifest.parent)
        Files.writeString(manifest, "$content\n")
        return directory
    }

    private fun load(vararg directories: Path): TypeRegistry = URLClassLoader(
        directories.map { it.toUri().toURL() }.toTypedArray(),
        javaClass.classLoader,
    ).use { classLoader ->
        TypeRegistry.load(classLoader)
    }

    private class ReviewStep
    private class NotificationJob
}
