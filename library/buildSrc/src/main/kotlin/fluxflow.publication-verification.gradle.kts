import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

// A module's Spring line is defined by the tree it lives in. Core modules (null)
// are Spring-agnostic and must not depend on either line.
fun springLineOf(projectPath: String) = when {
    projectPath.startsWith(":springboot4:") -> "-spring4"
    projectPath.startsWith(":springboot:") -> "-spring3"
    else -> null
}

val verifyPublications = tasks.register("verifyPublications") {
    group = "verification"
    description = "Verifies that all publications use the expected -spring3/-spring4 dual-line coordinates."
    doLast {
        val violations = mutableListOf<String>()
        val publishedProjects = project.subprojects.filter { it.plugins.hasPlugin("maven-publish") }

        publishedProjects.forEach { publishedProject ->
            val line = springLineOf(publishedProject.path)
            val expectedArtifactId = publishedProject.name + (line ?: "")
            val pluginMarkerArtifactIds = publishedProject.extensions
                .findByType(GradlePluginDevelopmentExtension::class.java)
                ?.plugins
                ?.map { "${it.id}.gradle.plugin" }
                ?.toSet()
                .orEmpty()

            publishedProject.extensions.getByType(PublishingExtension::class.java)
                .publications
                .withType(MavenPublication::class.java)
                .filter {
                    it.artifactId != expectedArtifactId &&
                            it.artifactId !in pluginMarkerArtifactIds
                }
                .forEach { publication ->
                    violations += "${publishedProject.path} publishes as \"${publication.artifactId}\"" +
                            " but must publish as \"$expectedArtifactId\"."
                }

            // Only the dependency scopes that end up in the published POM are relevant;
            // test dependencies may cross lines (e.g. core tests using springboot-testing).
            val publishedScopes = setOf("api", "compileOnlyApi", "implementation", "runtimeOnly")
            publishedProject.configurations
                .filter { it.name in publishedScopes }
                .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
                .map { it.path }
                .distinct()
                .filter { dependencyPath -> springLineOf(dependencyPath).let { it != null && it != line } }
                .forEach { dependencyPath ->
                    violations += "${publishedProject.path} must not depend on \"$dependencyPath\"" +
                            " (different Spring line)."
                }
        }

        val spring3Modules = publishedProjects.filter { it.path.startsWith(":springboot:") }.map { it.name }.toSet()
        val spring4Modules = publishedProjects.filter { it.path.startsWith(":springboot4:") }.map { it.name }.toSet()
        (spring3Modules - spring4Modules).forEach { moduleName ->
            violations += "Module \"$moduleName\" is missing from the -spring4 line (springboot4 tree)."
        }
        (spring4Modules - spring3Modules).forEach { moduleName ->
            violations += "Module \"$moduleName\" is missing from the -spring3 line (springboot tree)."
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Publication verification failed:\n" + violations.joinToString("\n") { " - $it" }
            )
        }
        val publicationCount = publishedProjects.sumOf { publishedProject ->
            publishedProject.extensions.getByType(PublishingExtension::class.java)
                .publications.withType(MavenPublication::class.java).size
        }
        logger.lifecycle("Verified $publicationCount publications: ${spring3Modules.size}x -spring3, ${spring4Modules.size}x -spring4.")
    }
}

// Runs with every regular build, so violations surface long before a release.
pluginManager.withPlugin("lifecycle-base") {
    tasks.named("check") {
        dependsOn(verifyPublications)
    }
}

// Remote publications (snapshot repository and Maven Central) must not run
// if the dual-line coordinate verification fails.
allprojects {
    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(verifyPublications)
    }
}
