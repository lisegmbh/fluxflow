package de.lise.fluxflow.mongo.flowquery.expression.compilation

import kotlin.reflect.KClass

interface SubclassProvider {
    fun findSubclasses(type: KClass<*>): Set<Class<*>>
}