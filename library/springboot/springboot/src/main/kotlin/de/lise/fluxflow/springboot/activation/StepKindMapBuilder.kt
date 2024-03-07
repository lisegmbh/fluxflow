package de.lise.fluxflow.springboot.activation

import de.lise.fluxflow.api.step.StepConfigurationException
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.stereotyped.step.Step
import de.lise.fluxflow.stereotyped.step.StepKindInspector
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.type.filter.AnnotationTypeFilter
import kotlin.reflect.KClass

/**
 * Scans the classpath for all classes annotated with [Step] and builds a map from [StepKind] to the class.
 * It determines the base packages to scan by looking at the [SpringBootApplication] and [ComponentScan] annotations.
 */
class StepKindMapBuilder(
    private val context: ApplicationContext,
    private val classLoader: ClassLoader,
) {
    fun build(): Map<StepKind, KClass<out Any>> {
        val componentProvider = ClassPathScanningCandidateComponentProvider(false)
        componentProvider.addIncludeFilter(AnnotationTypeFilter(Step::class.java))
        
        val basePackages = findBasePackages()

        val result = mutableMapOf<StepKind, KClass<out Any>>()
        basePackages.forEach {
            build(result, componentProvider, it)
        }

        return result
    }

    private fun findBasePackages(): Set<String> {
        val basePackagesFromSpringBootApplication =  context.getBeansWithAnnotation(SpringBootApplication::class.java)
            .map { context.findAnnotationOnBean(it.key, SpringBootApplication::class.java) }
            .filterNotNull()
            .flatMap { it.scanBasePackages.toList() + it.scanBasePackageClasses.map { clazz -> clazz.java.packageName } }
        
        val basePackagesFromComponentScan = context.getBeansWithAnnotation(ComponentScan::class.java)
            .map { context.findAnnotationOnBean(it.key, ComponentScan::class.java) }
            .filterNotNull()
            .flatMap { it.basePackages.toList() + it.basePackageClasses.map { clazz -> clazz.java.packageName } }
        
        return (basePackagesFromSpringBootApplication + basePackagesFromComponentScan).toSet()
    }

    private fun build(
        map: MutableMap<StepKind, KClass<out Any>>,
        scanningComponentProvider: ClassPathScanningCandidateComponentProvider,
        basePackage: String
    ) {
        val discoveredTypes = scanningComponentProvider.findCandidateComponents(basePackage)
            .filterIsInstance<AnnotatedBeanDefinition>()
            .map { it.beanClassName }
            .map { Class.forName(it, true, classLoader).kotlin }
            .associateBy { StepKindInspector.getStepKind(it) }

        for (entry in discoveredTypes) {
            val existing = map[entry.key]
            if (existing != null && existing != entry.value) {
                throw StepConfigurationException(
                    "The step kind '${entry.key.value}' is used on multiple classes " +
                            "(${existing.qualifiedName} and ${entry.value.qualifiedName})"
                )
            }
            map[entry.key] = entry.value
        }
    }
}