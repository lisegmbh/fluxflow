package de.fluxflow.flowquery.expression.compilation

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Discovers concrete subtypes of a [KClass] by scanning configured base packages.
 */
class ClasspathSubclassProvider(
    private val basePackages: Set<String>,
) : SubclassProvider {
    private val cache = ConcurrentHashMap<KClass<*>, Set<Class<*>>>()

    override fun findSubclasses(type: KClass<*>): Set<Class<*>> {
        return cache.computeIfAbsent(type) { findSubclassesUncached(it) }
    }

    private fun findSubclassesUncached(type: KClass<*>): Set<Class<*>> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AssignableTypeFilter(type.java))

        val allFoundClasses = basePackages.flatMap { scanner.findCandidateComponents(it) }
            .filter { !it.isAbstract }
            .mapNotNull { it.beanClassName }
            .distinct()
            .mapNotNull { runCatching { Class.forName(it) }.getOrNull() }

        val allClasses = (allFoundClasses + type.java).toSet()
        return allClasses.filter {
            !it.isInterface && !Modifier.isAbstract(it.modifiers)
        }.toSet()
    }
}
