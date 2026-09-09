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
            job.notification=example.NotificationJob
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
    fun `O06 should fail check when the jar contains the manifest more than once`() {
        fixture(
            declarations = "model 'external-model', 'example.ExternalModel'",
            jarConfiguration = """
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
                from(layout.buildDirectory.file(
                    'generated/fluxflowTypeManifest/META-INF/fluxflow/type-manifest.properties'
                )) {
                    into 'META-INF/fluxflow'
                }
            """.trimIndent(),
        )

        val result = run("check").buildAndFail()

        assertThat(result.task(":verifyFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains(
            "contains META-INF/fluxflow/type-manifest.properties more than once"
        )
    }

    @Test
    fun `O06 should generate and verify the manifest in an executable boot jar`() {
        fakeSpringBootPlugin()
        fixture(
            declarations = "model 'external-model', 'example.ExternalModel'",
            additionalPlugins = "id 'org.springframework.boot'",
        )

        val result = run("check").build()
        val bootManifest = manifestFromJar(
            "build/libs/manifest-fixture-boot.jar",
        )

        assertThat(result.task(":bootJar")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":verifyFluxflowTypeManifestBootJar")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(bootManifest.toString(Charsets.UTF_8)).contains(
            "model.external-model=example.ExternalModel"
        )
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

        val result = run("check").buildAndFail()

        assertThat(result.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("missing.DoesNotExist")
    }

    @Test
    fun `M03 O06 should reject an explicit type absent from the runtime classpath`() {
        fixture(declarations = "model 'external-model', 'external.ExternalModel'")
        externalTypeProject("compileOnly")

        val result = run("check").buildAndFail()

        assertThat(result.task(":generateFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("external.ExternalModel")
    }

    @Test
    fun `M03 O06 should accept an explicit type present on the runtime classpath`() {
        fixture(declarations = "model 'external-model', 'external.ExternalModel'")
        externalTypeProject("runtimeOnly")

        val result = run("check").build()

        assertThat(result.task(":verifyFluxflowTypeManifest")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(manifestFromJar().toString(Charsets.UTF_8)).contains(
            "model.external-model=external.ExternalModel"
        )
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

    private fun fakeSpringBootPlugin() {
        File(projectDir, "buildSrc/build.gradle").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins {
                    id 'java-gradle-plugin'
                }

                gradlePlugin {
                    plugins {
                        fakeSpringBoot {
                            id = 'org.springframework.boot'
                            implementationClass = 'fixture.FakeSpringBootPlugin'
                        }
                    }
                }
                """.trimIndent()
            )
        }
        File(projectDir, "buildSrc/src/main/java/fixture/FakeSpringBootPlugin.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package fixture;

                import org.gradle.api.Plugin;
                import org.gradle.api.Project;
                import org.gradle.api.tasks.SourceSet;
                import org.gradle.api.tasks.SourceSetContainer;
                import org.gradle.api.tasks.bundling.Jar;

                public class FakeSpringBootPlugin implements Plugin<Project> {
                    @Override
                    public void apply(Project project) {
                        SourceSetContainer sourceSets = project.getExtensions()
                            .getByType(SourceSetContainer.class);
                        project.getTasks().register("bootJar", Jar.class, task -> {
                            task.getArchiveClassifier().set("boot");
                            var mainOutput = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME)
                                .map(SourceSet::getOutput);
                            task.into("BOOT-INF/classes", copy -> {
                                copy.from(mainOutput);
                                copy.exclude("META-INF/**");
                            });
                            task.from(mainOutput, copy -> copy.include("META-INF/**"));
                        });
                    }
                }
                """.trimIndent()
            )
        }
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
