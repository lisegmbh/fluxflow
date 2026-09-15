package de.lise.fluxflow.gradle.manifest

import de.lise.fluxflow.reflection.types.TypeManifest
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.stereotyped.job.Job
import de.lise.fluxflow.stereotyped.step.Step
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files

@CacheableTask
abstract class GenerateFluxFlowTypeManifest : DefaultTask() {
    @get:Input
    abstract val declarations: ListProperty<String>

    @get:Input
    abstract val sourceProjectPath: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classesDirectories: ConfigurableFileCollection

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val urls = (classesDirectories.files + classpath.files)
            .distinct()
            .map { it.toURI().toURL() }
            .toTypedArray()

        URLClassLoader(urls, javaClass.classLoader).use { classLoader ->
            val explicitEntries = declarations.get().map { encoded ->
                val declaration = ManifestDeclarationCodec.decode(encoded)
                TypeManifestEntry(
                    declaration.role,
                    declaration.key,
                    declaration.binaryClassName,
                    "Gradle declaration in '${sourceProjectPath.get()}'",
                )
            }
            val scannedEntries = classesDirectories.files
                .filter(File::isDirectory)
                .flatMap { directory -> scan(directory, classLoader) }
            val entries = explicitEntries + scannedEntries

            TypeRegistry.create(classLoader, entries)
            val content = TypeManifest.write(entries)
            val target = outputFile.get().asFile.toPath()
            Files.createDirectories(target.parent)
            Files.writeString(target, content, Charsets.UTF_8)
        }
    }

    private fun scan(directory: File, classLoader: ClassLoader): List<TypeManifestEntry> =
        Files.walk(directory.toPath()).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .filter { it.fileName.toString().endsWith(".class") }
                .map { directory.toPath().relativize(it).toString() }
                .map { it.removeSuffix(".class").replace(File.separatorChar, '.') }
                .filter { it != "module-info" && !it.endsWith(".package-info") }
                .map { className -> inspect(className, classLoader) }
                .filter { it != null }
                .map { it!! }
                .toList()
        }

    private fun inspect(className: String, classLoader: ClassLoader): TypeManifestEntry? {
        val type = try {
            Class.forName(className, false, classLoader)
        } catch (exception: ClassNotFoundException) {
            throw TypeManifestException("Could not inspect compiled class '$className'.", exception)
        } catch (error: LinkageError) {
            throw TypeManifestException("Could not inspect compiled class '$className'.", error)
        }
        val step = type.getAnnotation(Step::class.java)
        val job = type.getAnnotation(Job::class.java)
        if (step != null && job != null) {
            throw TypeManifestException("Compiled class '$className' declares both @Step and @Job.")
        }
        val role = when {
            step != null -> TypeRole.STEP
            job != null -> TypeRole.JOB
            else -> return null
        }
        val alias = step?.kind ?: job?.kind.orEmpty()
        val key = alias.takeUnless(String::isBlank)
            ?: type.kotlin.qualifiedName
            ?: throw TypeManifestException("Compiled class '$className' does not have a qualified name.")
        return TypeManifestEntry(role, key, type.name, "annotated class '$className'")
    }
}
