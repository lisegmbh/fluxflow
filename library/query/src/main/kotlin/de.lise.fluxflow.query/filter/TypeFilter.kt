package de.lise.fluxflow.query.filter

import kotlin.reflect.KClass

class TypeFilter(
    val type: KClass<*>
) : Filter<KClass<*>>