package de.fluxflow.flowquery.expression.compilation

import kotlin.reflect.KClass

/**
 * Provides the set of concrete types considered subtypes of a given type.
 * Used by [de.fluxflow.flowquery.expression.CastExpression] sorting and [de.fluxflow.flowquery.expression.IsTypeExpression] checks.
 */
interface SubclassProvider {
    fun findSubclasses(type: KClass<*>): Set<Class<*>>
}
