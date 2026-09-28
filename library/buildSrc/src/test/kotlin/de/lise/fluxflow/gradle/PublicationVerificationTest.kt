package de.lise.fluxflow.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PublicationVerificationTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `verifyPublications should accept a build that follows the dual-line convention`() {
        // The cross-line test dependency must be tolerated (e.g. core tests using springboot-testing).
        fixture(
            Module("core:api", "api", testDependencies = listOf(":springboot:springboot-web")),
            Module("springboot:springboot-web", "springboot-web-spring3"),
            Module("springboot4:springboot-web", "springboot-web-spring4"),
        )

        val result = verify().build()

        assertThat(result.output).contains("Verified 3 publications: 1x -spring3, 1x -spring4.")
    }

    @Test
    fun `verifyPublications should reject a publication whose artifactId is missing the line suffix`() {
        fixture(
            Module("springboot:springboot-web", "springboot-web"),
            Module("springboot4:springboot-web", "springboot-web-spring4"),
        )

        val result = verify().buildAndFail()

        assertThat(result.output).contains(
            ":springboot:springboot-web publishes as \"springboot-web\"" +
                    " but must publish as \"springboot-web-spring3\"."
        )
    }

    @Test
    fun `verifyPublications should reject a module that only exists in one Spring line`() {
        fixture(
            Module("springboot:springboot-web", "springboot-web-spring3"),
            Module("springboot:springboot-extra", "springboot-extra-spring3"),
            Module("springboot4:springboot-web", "springboot-web-spring4"),
        )

        val result = verify().buildAndFail()

        assertThat(result.output).contains(
            "Module \"springboot-extra\" is missing from the -spring4 line (springboot4 tree)."
        )
    }

    @Test
    fun `verifyPublications should reject a published dependency onto the other Spring line`() {
        fixture(
            Module("springboot:springboot-web", "springboot-web-spring3"),
            Module(
                "springboot4:springboot-web",
                "springboot-web-spring4",
                dependencies = listOf(":springboot:springboot-web"),
            ),
        )

        val result = verify().buildAndFail()

        assertThat(result.output).contains(
            ":springboot4:springboot-web must not depend on \":springboot:springboot-web\" (different Spring line)."
        )
    }

    @Test
    fun `verifyPublications should reject a core module depending on a Spring line`() {
        fixture(
            Module("core:api", "api", dependencies = listOf(":springboot:springboot-web")),
            Module("springboot:springboot-web", "springboot-web-spring3"),
            Module("springboot4:springboot-web", "springboot-web-spring4"),
        )

        val result = verify().buildAndFail()

        assertThat(result.output).contains(
            ":core:api must not depend on \":springboot:springboot-web\" (different Spring line)."
        )
    }

    @Test
    fun `verifyPublications should accept a declared Gradle plugin marker`() {
        fixture(
            Module(
                "manifest-gradle-plugin",
                "manifest-gradle-plugin",
                pluginId = "de.lise.fluxflow.type-manifest",
            )
        )

        val result = verify(
            "verifyPublications",
            ":manifest-gradle-plugin:generatePomFileForFixturePluginPluginMarkerMavenPublication",
        ).build()
        val markerPom = File(
            projectDir,
            "manifest-gradle-plugin/build/publications/fixturePluginPluginMarkerMaven/pom-default.xml",
        ).readText()

        assertThat(result.output).contains("Verified 3 publications: 0x -spring3, 0x -spring4.")
        assertThat(markerPom)
            .contains("<groupId>de.lise.fluxflow.type-manifest</groupId>")
            .contains("<artifactId>de.lise.fluxflow.type-manifest.gradle.plugin</artifactId>")
            .contains("<artifactId>manifest-gradle-plugin</artifactId>")
    }

    private data class Module(
        val path: String,
        val artifactId: String,
        val dependencies: List<String> = emptyList(),
        val testDependencies: List<String> = emptyList(),
        val pluginId: String? = null,
    )

    private fun fixture(vararg modules: Module) {
        File(projectDir, "settings.gradle").writeText(
            "rootProject.name = 'fixture'\n" +
                    modules.joinToString("\n") { "include '${it.path}'" }
        )
        File(projectDir, "build.gradle").writeText(
            "plugins { id 'fluxflow.publication-verification' }"
        )
        modules.forEach { module ->
            val moduleDir = File(projectDir, module.path.replace(':', '/'))
            moduleDir.mkdirs()
            val dependencies = module.dependencies.map { "implementation project('$it')" } +
                    module.testDependencies.map { "testImplementation project('$it')" }
            File(moduleDir, "build.gradle").writeText(
                """
                plugins {
                    id 'java-library'
                    id 'maven-publish'
                    ${if (module.pluginId != null) "id 'java-gradle-plugin'" else ""}
                }
                group = 'de.lise.fluxflow'
                version = '1.0.0'
                publishing {
                    publications {
                        maven(MavenPublication) {
                            from components.java
                            artifactId = '${module.artifactId}'
                        }
                    }
                }
                ${module.pluginId?.let { pluginId ->
                    """
                    gradlePlugin {
                        plugins {
                            fixturePlugin {
                                id = '$pluginId'
                                implementationClass = 'example.FixturePlugin'
                            }
                        }
                    }
                    """.trimIndent()
                }.orEmpty()}
                dependencies {
                    ${dependencies.joinToString("\n                    ")}
                }
                """.trimIndent()
            )
        }
    }

    private fun verify(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*(arguments.takeIf { it.isNotEmpty() } ?: arrayOf("verifyPublications")))
}
