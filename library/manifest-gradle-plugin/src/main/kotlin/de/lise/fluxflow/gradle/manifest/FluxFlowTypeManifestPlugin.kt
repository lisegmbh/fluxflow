package de.lise.fluxflow.gradle.manifest

import de.lise.fluxflow.reflection.types.TypeManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
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
        val generate = project.tasks.register(
            "generateFluxflowTypeManifest",
            GenerateFluxFlowTypeManifest::class.java,
        ) { task ->
            task.group = "build"
            task.description = "Generates the trusted FluxFlow type manifest."
            task.declarations.set(extension.declarations)
            task.sourceProjectPath.set(project.path)
            task.classesDirectories.from(main.map { it.output.classesDirs })
            task.classpath.from(main.map { it.compileClasspath }, main.map { it.output.classesDirs })
            task.outputFile.convention(
                project.layout.buildDirectory.file(
                    "generated/fluxflowTypeManifest/type-manifest.properties"
                )
            )
        }
        val jar = project.tasks.named("jar", Jar::class.java) { task ->
            task.dependsOn(generate)
            task.from(generate.flatMap { it.outputFile }) { copy ->
                copy.into(TypeManifest.RESOURCE_PATH.substringBeforeLast('/'))
            }
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
        }
        project.tasks.named("check") { it.dependsOn(verify) }
    }
}
