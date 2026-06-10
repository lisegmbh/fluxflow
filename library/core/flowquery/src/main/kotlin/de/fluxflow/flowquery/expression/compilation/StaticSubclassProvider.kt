package de.fluxflow.flowquery.expression.compilation

import kotlin.reflect.KClass

/**
 * [SubclassProvider] with an explicit registry, primarily for unit tests.
 */
class StaticSubclassProvider(
    private val subclassesByType: Map<KClass<*>, Set<Class<*>>> = emptyMap(),
) : SubclassProvider {
    override fun findSubclasses(type: KClass<*>): Set<Class<*>> {
        return subclassesByType[type] ?: setOf(type.java)
    }
}
