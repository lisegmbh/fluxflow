package de.lise.fluxflow.gradle.manifest

import de.lise.fluxflow.reflection.types.TypeManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar

class FluxFlowTypeManifestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(JavaPlugin::class.java)
        val extension = project.extensions.create(
            "fluxflowTypeManifest",
            FluxFlowTypeManifestExtension::class.java,
        )
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val main = sourceSets.named("main")
        val generatedResourcesDirectory = project.layout.buildDirectory.dir(
            "generated/fluxflowTypeManifest"
        )
        val generate = project.tasks.register(
            "generateFluxflowTypeManifest",
            GenerateFluxFlowTypeManifest::class.java,
        ) { task ->
            task.group = "build"
            task.description = "Generates the trusted FluxFlow type manifest."
            task.declarations.set(extension.declarations)
            task.sourceProjectPath.set(project.path)
            task.classesDirectories.from(main.map { it.output.classesDirs })
            task.classpath.from(
                project.configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME),
                main.map { it.output.classesDirs },
            )
            task.outputFile.convention(
                generatedResourcesDirectory.map { directory ->
                    directory.file(TypeManifest.RESOURCE_PATH)
                }
            )
        }
        main.configure { sourceSet ->
            sourceSet.output.dir(
                mapOf("builtBy" to generate),
                generatedResourcesDirectory,
            )
        }
        val jar = project.tasks.named("jar", Jar::class.java) { task ->
            task.dependsOn(generate)
        }
        val verify = project.tasks.register(
            "verifyFluxflowTypeManifest",
            VerifyFluxFlowTypeManifest::class.java,
        ) { task ->
            task.group = "verification"
            task.description = "Verifies the trusted FluxFlow type manifest in the application JAR."
            task.dependsOn(jar)
            task.generatedManifest.set(generate.flatMap { it.outputFile })
            task.archiveFile.set(jar.flatMap { it.archiveFile })
            task.archiveEntryPath.convention(TypeManifest.RESOURCE_PATH)
        }
        project.tasks.named("check") { it.dependsOn(verify) }

        project.pluginManager.withPlugin("org.springframework.boot") {
            val bootJar = project.tasks.named(
                "bootJar",
                AbstractArchiveTask::class.java,
            ) { task ->
                task.dependsOn(generate)
            }
            val verifyBootJar = project.tasks.register(
                "verifyFluxflowTypeManifestBootJar",
                VerifyFluxFlowTypeManifest::class.java,
            ) { task ->
                task.group = "verification"
                task.description = "Verifies the trusted FluxFlow type manifest in the executable Boot JAR."
                task.dependsOn(bootJar)
                task.generatedManifest.set(generate.flatMap { it.outputFile })
                task.archiveFile.set(bootJar.flatMap { it.archiveFile })
                task.archiveEntryPath.set(TypeManifest.RESOURCE_PATH)
            }
            project.tasks.named("check") { it.dependsOn(verifyBootJar) }
        }
    }
}
