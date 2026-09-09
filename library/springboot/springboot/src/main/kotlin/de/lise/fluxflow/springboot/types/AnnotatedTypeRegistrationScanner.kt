package de.lise.fluxflow.springboot.types

import de.lise.fluxflow.reflection.types.TypeManifestEntry
import de.lise.fluxflow.reflection.types.TypeManifestException
import de.lise.fluxflow.reflection.types.TypeRole
import de.lise.fluxflow.stereotyped.job.Job
import de.lise.fluxflow.stereotyped.step.Step
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter

/**
 * Discovers annotated step and job declarations without initializing their classes.
 */
class AnnotatedTypeRegistrationScanner(
    private val classLoader: ClassLoader,
) {
    fun scan(basePackages: Iterable<String>): List<TypeManifestEntry> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.setResourceLoader(PathMatchingResourcePatternResolver(classLoader))
        scanner.addIncludeFilter(AnnotationTypeFilter(Step::class.java, false))
        scanner.addIncludeFilter(AnnotationTypeFilter(Job::class.java, false))

        return basePackages
            .flatMap { basePackage ->
                scanner.findCandidateComponents(basePackage)
                    .filterIsInstance<AnnotatedBeanDefinition>()
                    .map { definition -> registration(definition.metadata, basePackage) }
            }
            .distinctBy { Triple(it.role, it.key, it.binaryClassName) }
            .sortedWith(
                compareBy<TypeManifestEntry> { it.role.ordinal }
                    .thenBy { it.key }
                    .thenBy { it.binaryClassName }
            )
    }

    private fun registration(
        metadata: AnnotationMetadata,
        basePackage: String,
    ): TypeManifestEntry {
        val isStep = metadata.isAnnotated(Step::class.java.name)
        val isJob = metadata.isAnnotated(Job::class.java.name)
        val role = when {
            isStep && isJob -> throw TypeManifestException(
                "Scanned type '${metadata.className}' declares both @Step and @Job."
            )
            isStep -> TypeRole.STEP
            isJob -> TypeRole.JOB
            else -> throw TypeManifestException(
                "Scanned type '${metadata.className}' is neither a FluxFlow step nor a job."
            )
        }
        val annotationName = when (role) {
            TypeRole.STEP -> Step::class.java.name
            TypeRole.JOB -> Job::class.java.name
            else -> error("Only step and job annotations are scanned.")
        }
        val alias = metadata.getAnnotationAttributes(annotationName, false)
            ?.get("kind")
            ?.toString()
            .orEmpty()
            .takeUnless(String::isBlank)
        val binaryClassName = metadata.className
        val key = alias ?: try {
            Class.forName(binaryClassName, false, classLoader).kotlin.qualifiedName
                ?: throw TypeManifestException(
                    "Scanned type '$binaryClassName' does not have a qualified name."
                )
        } catch (exception: ClassNotFoundException) {
            throw TypeManifestException(
                "Could not resolve scanned type '$binaryClassName' from package '$basePackage'.",
                exception,
            )
        } catch (error: LinkageError) {
            throw TypeManifestException(
                "Could not resolve scanned type '$binaryClassName' from package '$basePackage'.",
                error,
            )
        }

        return TypeManifestEntry(
            role,
            key,
            binaryClassName,
            "annotation scan of '$basePackage'",
        )
    }
}
