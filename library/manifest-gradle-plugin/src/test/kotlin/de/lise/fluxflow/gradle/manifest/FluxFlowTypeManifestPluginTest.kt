package de.lise.fluxflow.gradle.manifest

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class FluxFlowTypeManifestPluginTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `M02 O06 should generate deterministic manifest bytes and include them in the jar`() {
        fixture(
            declarations = """
                model 'external-model', 'example.ExternalModel'
                value 'currency', 'java.lang.String'
            """.trimIndent()
        )

        val firstResult = run("check", "--rerun-tasks", "--configuration-cache").build()
        val firstManifest = manifestFromJar()

        fixture(
            declarations = """
                value 'currency', 'java.lang.String'
                model 'external-model', 'example.ExternalModel'
            """.trimIndent()
        )
        val secondResult = run("check", "--rerun-tasks", "--configuration-cache").build()
        val secondManifest = manifestFromJar()

        assertThat(firstResult.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(firstResult.task(":verifyFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(secondResult.task(":verifyFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(firstManifest).isEqualTo(secondManifest)
        assertThat(firstManifest.toString(Charsets.UTF_8)).isEqualTo(
            """
            manifest.version=1
            step.review=example.ReviewStep
            model.external-model=example.ExternalModel
            value.currency=java.lang.String
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `O06 should fail check when the jar omits the generated manifest`() {
        fixture(
            declarations = "model 'external-model', 'example.ExternalModel'",
            jarConfiguration = """
                eachFile {
                    if (path == 'META-INF/fluxflow/type-manifest.properties') {
                        exclude()
                    }
                }
            """.trimIndent(),
        )

        val result = run("check").buildAndFail()

        assertThat(result.task(":verifyFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("does not contain META-INF/fluxflow/type-manifest.properties")
    }

    @Test
    fun `M03 O06 should fail generation when an explicit class is missing`() {
        fixture(declarations = "model 'missing', 'missing.DoesNotExist'")

        val result = run("check").buildAndFail()

        assertThat(result.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("missing.DoesNotExist")
    }

    @Test
    fun `M04 should inspect annotated classes without initialization`() {
        val marker = File(projectDir, "initialized.txt")
        fixture(declarations = "model 'external-model', 'example.ExternalModel'")

        val result = run("check", "-Dfluxflow.test.marker=${marker.absolutePath}").build()

        assertThat(result.task(":verifyFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(marker).doesNotExist()
    }

    private fun fixture(
        declarations: String,
        jarConfiguration: String = "",
    ) {
        File(projectDir, "settings.gradle").writeText("rootProject.name = 'manifest-fixture'")
        File(projectDir, "build.gradle").writeText(
            """
            plugins {
                id 'java'
                id 'de.lise.fluxflow.type-manifest'
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation files('${escapedPath(System.getProperty("fluxflow.test.stereotypedJar"))}')
            }

            fluxflowTypeManifest {
                $declarations
            }

            tasks.named('jar') {
                $jarConfiguration
            }
            """.trimIndent()
        )
        val sourceDir = File(projectDir, "src/main/java/example").apply { mkdirs() }
        File(sourceDir, "ReviewStep.java").writeText(
            """
            package example;

            import de.lise.fluxflow.stereotyped.step.Step;
            import java.nio.file.Files;
            import java.nio.file.Path;

            @Step(kind = "review")
            public class ReviewStep {
                static {
                    String marker = System.getProperty("fluxflow.test.marker");
                    if (marker != null) {
                        try {
                            Files.writeString(Path.of(marker), "initialized");
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                }
            }
            """.trimIndent()
        )
        File(sourceDir, "ExternalModel.java").writeText(
            """
            package example;

            public class ExternalModel {
            }
            """.trimIndent()
        )
    }

    private fun run(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")

    private fun manifestFromJar(): ByteArray {
        val jar = File(projectDir, "build/libs/manifest-fixture.jar")
        return ZipFile(jar).use { archive ->
            archive.getInputStream(
                archive.getEntry("META-INF/fluxflow/type-manifest.properties")
            ).readAllBytes()
        }
    }

    private fun escapedPath(value: String): String = value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
}
