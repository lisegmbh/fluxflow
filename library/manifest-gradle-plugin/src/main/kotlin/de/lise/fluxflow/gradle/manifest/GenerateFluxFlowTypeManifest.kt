package de.lise.fluxflow.gradle.manifest

import de.lise.fluxflow.reflection.types.TypeManifest
import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRegistry
import de.lise.fluxflow.reflection.types.TypeRole
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
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
    abstract val declarations: ListProperty<ManifestDeclaration>

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

        URLClassLoader(urls, ClassLoader.getPlatformClassLoader()).use { classLoader ->
            val explicitEntries = declarations.get().map { declaration ->
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
            val content = TypeManifest.write(entries, coveredPackages())
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
                .map { path ->
                    Files.newInputStream(path).use { inspect(ClassReader(it), classLoader) }
                }
                .filter { it != null }
                .map { it!! }
                .toList()
        }

    private fun coveredPackages(): List<String> =
        classesDirectories.files
            .filter(File::isDirectory)
            .flatMap { directory ->
                Files.walk(directory.toPath()).use { paths ->
                    paths
                        .filter(Files::isRegularFile)
                        .filter { it.fileName.toString().endsWith(".class") }
                        .map { directory.toPath().relativize(it).toString() }
                        .map { it.removeSuffix(".class").replace(File.separatorChar, '.') }
                        .filter { it != "module-info" && !it.endsWith(".package-info") }
                        .map { className -> className.substringBeforeLast('.', missingDelimiterValue = "") }
                        .filter { it.isNotEmpty() }
                        .toList()
                }
            }
            .distinct()
            .sorted()

    private fun inspect(bytecode: ClassReader, classLoader: ClassLoader): TypeManifestEntry? {
        val annotations = mutableMapOf<TypeRole, String>()
        bytecode.accept(object : ClassVisitor(Opcodes.ASM9) {
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                val role = when (descriptor) {
                    "Lde/lise/fluxflow/stereotyped/step/Step;" -> TypeRole.STEP
                    "Lde/lise/fluxflow/stereotyped/job/Job;" -> TypeRole.JOB
                    else -> return null
                }
                annotations[role] = ""
                return object : AnnotationVisitor(Opcodes.ASM9) {
                    override fun visit(name: String, value: Any) {
                        if (name == "kind") annotations[role] = value as String
                    }
                }
            }
        }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        val className = bytecode.className.replace('/', '.')
        if (annotations.size > 1) {
            throw TypeManifestException("Compiled class '$className' declares both @Step and @Job.")
        }
        val (role, alias) = annotations.entries.singleOrNull() ?: return null
        val key = alias.takeUnless(String::isBlank) ?: try {
            Class.forName(className, false, classLoader).kotlin.qualifiedName
                ?: throw TypeManifestException("Compiled class '$className' does not have a qualified name.")
        } catch (exception: ClassNotFoundException) {
            throw TypeManifestException("Could not inspect compiled class '$className'.", exception)
        } catch (error: LinkageError) {
            throw TypeManifestException("Could not inspect compiled class '$className'.", error)
        }
        return TypeManifestEntry(role, key, className, "annotated class '$className'")
    }
}
