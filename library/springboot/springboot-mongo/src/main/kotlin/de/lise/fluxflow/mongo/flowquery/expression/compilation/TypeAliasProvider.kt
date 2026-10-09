package de.lise.fluxflow.mongo.flowquery.expression.compilation

import kotlin.reflect.KClass

/** Optional query capability for inventories accepting aliases beyond JVM class names. */
interface TypeAliasProvider {
    fun findTypeAliases(type: KClass<*>): Set<String>
}
