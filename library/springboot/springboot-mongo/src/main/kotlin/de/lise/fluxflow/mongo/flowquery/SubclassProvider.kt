package de.lise.fluxflow.mongo.flowquery

import kotlin.reflect.KClass

interface SubclassProvider {
    fun findSubclasses(type: KClass<*>): Set<Class<*>>
}