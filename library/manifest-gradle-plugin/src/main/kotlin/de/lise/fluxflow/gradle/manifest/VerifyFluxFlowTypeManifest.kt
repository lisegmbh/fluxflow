package de.lise.fluxflow.gradle.manifest

import de.lise.fluxflow.reflection.types.TypeManifest
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.zip.ZipFile

@DisableCachingByDefault(because = "Manifest verification has no output artifact.")
abstract class VerifyFluxFlowTypeManifest : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val generatedManifest: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val archiveFile: RegularFileProperty

    @get:Input
    abstract val archiveEntryPath: Property<String>

    @TaskAction
    fun verify() {
        val expected = generatedManifest.get().asFile.readBytes()
        val archive = archiveFile.get().asFile
        val expectedEntryPath = archiveEntryPath.get()
        ZipFile(archive).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { it.name == expectedEntryPath }
                .toList()
            if (entries.isEmpty()) {
                throw GradleException(
                    "Archive '${archive.name}' does not contain $expectedEntryPath."
                )
            }
            if (entries.size > 1) {
                throw GradleException(
                    "Archive '${archive.name}' contains $expectedEntryPath more than once."
                )
            }
            val actual = zip.getInputStream(entries.single()).use { it.readAllBytes() }
            if (!actual.contentEquals(expected)) {
                throw GradleException(
                    "Archive '${archive.name}' contains a stale $expectedEntryPath."
                )
            }
        }
    }
}
