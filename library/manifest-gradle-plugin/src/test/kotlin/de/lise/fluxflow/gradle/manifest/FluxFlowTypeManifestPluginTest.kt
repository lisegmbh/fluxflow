package de.lise.fluxflow.gradle.manifest

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.util.zip.ZipFile
import java.util.concurrent.TimeUnit

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

        val firstResult = run("jar", "--rerun-tasks", "--configuration-cache").build()
        val firstManifest = manifestFromJar()

        fixture(
            declarations = """
                value 'currency', 'java.lang.String'
                model 'external-model', 'example.ExternalModel'
            """.trimIndent()
        )
        val secondResult = run("jar", "--rerun-tasks", "--configuration-cache").build()
        val secondManifest = manifestFromJar()

        assertThat(firstResult.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(firstResult.task(":jar")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(secondResult.task(":jar")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(firstManifest).isEqualTo(secondManifest)
        assertThat(firstManifest.toString(Charsets.UTF_8)).isEqualTo(
            """
            manifest.version=1
            manifest.covered-packages=example
            step.review=example.ReviewStep
            job.notification=example.NotificationJob
            model.external-model=example.ExternalModel
            value.currency=java.lang.String
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `should not register archive verification tasks`() {
        fixture(
            declarations = "model 'external-model', 'example.ExternalModel'",
            additionalPlugins = "id 'org.springframework.boot' version '3.5.7'",
        )

        val result = run("tasks", "--all").build()

        assertThat(result.output)
            .doesNotContain("verifyFluxflowTypeManifest")
            .doesNotContain("verifyFluxflowTypeManifestBootJar")
    }

    @ParameterizedTest
    @ValueSource(strings = ["3.5.7", "4.0.6"])
    fun `O06 should generate the manifest in an executable boot jar`(bootVersion: String) {
        fixture(
            declarations = "model 'external-model', 'example.ExternalModel'",
            additionalPlugins = "id 'org.springframework.boot' version '$bootVersion'",
        )
        runtimeManifestProbe()
        File(projectDir, "build.gradle").appendText(
            """

            tasks.named('bootJar') {
                mainClass = 'example.ManifestRuntimeProbe'
                archiveClassifier = 'boot'
            }
            """.trimIndent()
        )

        val result = run("bootJar").build()
        val bootManifest = manifestFromJar(
            "build/libs/manifest-fixture-boot.jar",
        )

        assertThat(result.task(":bootJar")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":verifyFluxflowTypeManifestBootJar")).isNull()
        assertThat(bootManifest.toString(Charsets.UTF_8)).contains(
            "model.external-model=example.ExternalModel"
        )
        val executable = File(System.getProperty("java.home"),
            "bin/java" + if (File.separatorChar == '\\') ".exe" else "")
        val output = File(projectDir, "boot-runtime-output.txt")
        val process = ProcessBuilder(executable.absolutePath, "-jar",
            File(projectDir, "build/libs/manifest-fixture-boot.jar").absolutePath)
            .redirectErrorStream(true).redirectOutput(output).start()
        try {
            assertThat(process.waitFor(60, TimeUnit.SECONDS)).isTrue()
            assertThat(process.exitValue()).withFailMessage(output.readText()).isZero()
        } finally {
            process.destroyForcibly()
        }
    }

    @Test
    fun `O06 should expose the generated manifest on the main runtime classpath`() {
        fixture(declarations = "model 'external-model', 'example.ExternalModel'")
        runtimeManifestProbe()

        val result = run("verifyRuntimeManifest").build()

        assertThat(result.task(":verifyRuntimeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `M03 O06 should fail generation when an explicit class is missing`() {
        fixture(declarations = "model 'missing', 'missing.DoesNotExist'")

        val result = run("generateFluxflowTypeManifest").buildAndFail()

        assertThat(result.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("missing.DoesNotExist")
    }

    @Test
    fun `M03 O06 should reject an explicit type absent from the runtime classpath`() {
        fixture(declarations = "model 'external-model', 'external.ExternalModel'")
        externalTypeProject("compileOnly")

        val result = run("generateFluxflowTypeManifest").buildAndFail()

        assertThat(result.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("external.ExternalModel")
    }

    @Test
    fun `M03 O06 should accept an explicit type present on the runtime classpath`() {
        fixture(declarations = "model 'external-model', 'external.ExternalModel'")
        externalTypeProject("runtimeOnly")

        val result = run("jar").build()

        assertThat(result.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(manifestFromJar().toString(Charsets.UTF_8)).contains(
            "model.external-model=external.ExternalModel"
        )
    }

    @Test
    fun `M04 should inspect annotated classes without initialization`() {
        val marker = File(projectDir, "initialized.txt")
        fixture(declarations = "model 'external-model', 'example.ExternalModel'")

        val result = run("jar", "-Dfluxflow.test.marker=${marker.absolutePath}").build()

        assertThat(result.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(marker).doesNotExist()
    }

    @Test
    fun `should ignore unrelated classes with compileOnly supertypes`() {
        fixture(declarations = "")
        externalTypeProject("compileOnly")
        File(projectDir, "src/main/java/example/UnrelatedFilter.java").writeText(
            """
            package example;
            public class UnrelatedFilter extends external.ExternalModel {}
            """.trimIndent()
        )

        run("jar").build()

        assertThat(manifestFromJar().toString(Charsets.UTF_8)).isEqualTo(
            "manifest.version=1\nmanifest.covered-packages=example\nstep.review=example.ReviewStep\njob.notification=example.NotificationJob\n"
        )
    }

    @Test
    fun `should reject registered classes available only to the plugin loader`() {
        fixture(declarations = "model 'plugin-only', 'de.lise.fluxflow.reflection.types.TypeRegistry'")

        val result = run("generateFluxflowTypeManifest").buildAndFail()

        assertThat(result.output).contains("Could not resolve model type 'de.lise.fluxflow.reflection.types.TypeRegistry'")
    }

    @Test
    fun `should preserve nested default keys and binary class names`() {
        fixture(declarations = "")
        File(projectDir, "src/main/java/example/Outer.java").writeText(
            """
            package example;
            public class Outer {
                @de.lise.fluxflow.stereotyped.step.Step
                public static class Nested {}
            }
            """.trimIndent()
        )

        run("jar").build()

        assertThat(manifestFromJar().toString(Charsets.UTF_8))
            .contains("step.example.Outer.Nested=example.Outer${'$'}Nested\n")
    }

    @Test
    fun `should reject annotated classes with missing runtime supertypes`() {
        fixture(declarations = "")
        externalTypeProject("compileOnly")
        File(projectDir, "src/main/java/example/BrokenStep.java").writeText(
            """
            package example;
            @de.lise.fluxflow.stereotyped.step.Step(kind = "broken")
            public class BrokenStep extends external.ExternalModel {}
            """.trimIndent()
        )

        val result = run("generateFluxflowTypeManifest").buildAndFail()

        assertThat(result.output).contains("Could not resolve step type 'example.BrokenStep'")
    }

    @Test
    fun `should reject classes declaring both step and job roles`() {
        fixture(declarations = "")
        File(projectDir, "src/main/java/example/Ambiguous.java").writeText(
            """
            package example;
            @de.lise.fluxflow.stereotyped.step.Step(kind = "ambiguous")
            @de.lise.fluxflow.stereotyped.job.Job(kind = "ambiguous")
            public class Ambiguous {}
            """.trimIndent()
        )

        val result = run("generateFluxflowTypeManifest").buildAndFail()

        assertThat(result.output).contains("Compiled class 'example.Ambiguous' declares both @Step and @Job")
    }

    @ParameterizedTest
    @ValueSource(strings = ["9.2.0", "9.8.0"])
    fun `should support the documented Kotlin DSL on supported Gradle versions`(gradleVersion: String) {
        fixture(declarations = "")
        File(projectDir, "build.gradle").delete()
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                java
                id("de.lise.fluxflow.type-manifest")
            }
            repositories { mavenCentral() }
            dependencies {
                implementation(files("${escapedPath(System.getProperty("fluxflow.test.stereotypedJar"))}"))
            }
            fluxflowTypeManifest {
                model("external-model", "example.ExternalModel")
                value("currency", "java.lang.String")
            }
            """.trimIndent()
        )

        run("jar").withGradleVersion(gradleVersion).build()

        assertThat(manifestFromJar().toString(Charsets.UTF_8))
            .contains("model.external-model=example.ExternalModel\n", "value.currency=java.lang.String\n")
    }

    @Test
    fun `should keep declaration keys that contain a semicolon`() {
        fixture(declarations = "model 'semi;colon', 'example.ExternalModel'")

        run("jar").build()

        assertThat(manifestFromJar().toString(Charsets.UTF_8))
            .contains("model.semi;colon=example.ExternalModel\n")
    }

    @Test
    fun `should record compiled class packages as covered`() {
        fixture(declarations = "")
        File(projectDir, "src/main/java/other/OtherType.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package other;
                public class OtherType {}
                """.trimIndent()
            )
        }

        run("jar").build()

        assertThat(manifestFromJar().toString(Charsets.UTF_8))
            .contains("manifest.covered-packages=example,other\n")
    }

    private fun fixture(
        declarations: String,
        jarConfiguration: String = "",
        additionalPlugins: String = "",
    ) {
        File(projectDir, "settings.gradle").writeText("rootProject.name = 'manifest-fixture'")
        File(projectDir, "build.gradle").writeText(
            """
            plugins {
                id 'java'
                id 'de.lise.fluxflow.type-manifest'
                $additionalPlugins
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
        File(sourceDir, "NotificationJob.java").writeText(
            """
            package example;

            import de.lise.fluxflow.stereotyped.job.Job;

            @Job(kind = "notification")
            public class NotificationJob {
            }
            """.trimIndent()
        )
    }

    private fun externalTypeProject(configuration: String) {
        File(projectDir, "settings.gradle").appendText("\ninclude 'external-types'\n")
        File(projectDir, "build.gradle").appendText(
            """

            dependencies {
                $configuration project(':external-types')
            }
            """.trimIndent()
        )
        File(projectDir, "external-types/build.gradle").apply {
            parentFile.mkdirs()
            writeText("plugins { id 'java' }")
        }
        File(projectDir, "external-types/src/main/java/external/ExternalModel.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package external;

                public class ExternalModel {
                }
                """.trimIndent()
            )
        }
    }

    private fun runtimeManifestProbe() {
        File(projectDir, "build.gradle").appendText(
            """

            tasks.register('verifyRuntimeManifest', JavaExec) {
                classpath = sourceSets.main.runtimeClasspath
                mainClass = 'example.ManifestRuntimeProbe'
            }
            """.trimIndent()
        )
        File(projectDir, "src/main/java/example/ManifestRuntimeProbe.java").writeText(
            """
            package example;

            import java.nio.charset.StandardCharsets;

            public class ManifestRuntimeProbe {
                public static void main(String[] args) throws Exception {
                    try (var input = ManifestRuntimeProbe.class.getClassLoader().getResourceAsStream(
                        "META-INF/fluxflow/type-manifest.properties"
                    )) {
                        if (input == null) {
                            throw new IllegalStateException(
                                "Manifest is missing from the main runtime classpath."
                            );
                        }
                        var content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                        if (!content.contains("model.external-model=example.ExternalModel")) {
                            throw new IllegalStateException(
                                "Manifest does not contain the explicit model registration."
                            );
                        }
                    }
                }
            }
            """.trimIndent()
        )
    }

    private fun run(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")

    private fun manifestFromJar(
        archivePath: String = "build/libs/manifest-fixture.jar",
        manifestPath: String = "META-INF/fluxflow/type-manifest.properties",
    ): ByteArray {
        val jar = File(projectDir, archivePath)
        return ZipFile(jar).use { archive ->
            archive.getInputStream(
                archive.getEntry(manifestPath)
            ).readAllBytes()
        }
    }

    private fun escapedPath(value: String): String = value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
}
