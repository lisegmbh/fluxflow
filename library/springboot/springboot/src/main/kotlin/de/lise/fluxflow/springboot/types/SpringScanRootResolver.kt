package de.lise.fluxflow.springboot.types

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.type.AnnotationMetadata
import org.springframework.util.ClassUtils

/**
 * Resolves the effective packages declared by Spring Boot and component scan configuration.
 */
class SpringScanRootResolver(
    private val context: ApplicationContext,
) {
    fun resolve(): List<String> {
        val roots = mutableListOf<String>()
        val beanFactory = context.autowireCapableBeanFactory as? ConfigurableListableBeanFactory
            ?: return normalize(rootsFromContext())

        if (AutoConfigurationPackages.has(beanFactory)) {
            roots += AutoConfigurationPackages.get(beanFactory)
        }

        beanFactory.beanDefinitionNames
            .map(beanFactory::getBeanDefinition)
            .filterIsInstance<AnnotatedBeanDefinition>()
            .map { it.metadata }
            .forEach { metadata ->
                roots += rootsFrom(
                    metadata,
                    SpringBootApplication::class.java.name,
                    "scanBasePackages",
                    "scanBasePackageClasses",
                )
                roots += rootsFrom(
                    metadata,
                    ComponentScan::class.java.name,
                    "basePackages",
                    "basePackageClasses",
                )
            }

        return normalize(roots)
    }

    private fun rootsFrom(
        metadata: AnnotationMetadata,
        annotationName: String,
        packagesAttribute: String,
        packageClassesAttribute: String,
    ): List<String> {
        if (!metadata.isAnnotated(annotationName)) {
            return emptyList()
        }
        val attributes = metadata.getAnnotationAttributes(annotationName, true)
            ?: return emptyList()
        val explicitRoots = strings(attributes[packagesAttribute]) +
                strings(attributes[packageClassesAttribute]).map(ClassUtils::getPackageName)
        return explicitRoots.ifEmpty {
            listOf(ClassUtils.getPackageName(metadata.className))
        }
    }

    private fun rootsFromContext(): List<String> {
        val roots = mutableListOf<String>()
        context.getBeansWithAnnotation(SpringBootApplication::class.java).forEach { (name, bean) ->
            val annotation = context.findAnnotationOnBean(name, SpringBootApplication::class.java)
                ?: return@forEach
            roots += (annotation.scanBasePackages.toList() +
                annotation.scanBasePackageClasses.map { it.java.packageName }).ifEmpty {
                listOf((context.getType(name) ?: bean.javaClass).packageName)
            }
        }
        context.getBeansWithAnnotation(ComponentScan::class.java).forEach { (name, bean) ->
            val annotation = context.findAnnotationOnBean(name, ComponentScan::class.java)
                ?: return@forEach
            roots += (annotation.basePackages.toList() +
                annotation.basePackageClasses.map { it.java.packageName }).ifEmpty {
                listOf((context.getType(name) ?: bean.javaClass).packageName)
            }
        }
        return roots
    }

    private fun strings(value: Any?): List<String> = when (value) {
        is Array<*> -> value.filterIsInstance<String>()
        is String -> listOf(value)
        else -> emptyList()
    }

    private fun normalize(roots: Iterable<String>): List<String> = roots
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .sortedWith(compareBy<String> { it.count { character -> character == '.' } }.thenBy { it })
        .fold(mutableListOf()) { result, candidate ->
            if (result.none { candidate == it || candidate.startsWith("$it.") }) {
                result += candidate
            }
            result
        }
}
