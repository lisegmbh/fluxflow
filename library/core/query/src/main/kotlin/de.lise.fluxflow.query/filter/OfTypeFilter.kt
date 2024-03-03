package de.lise.fluxflow.query.filter

import kotlin.reflect.KClass

class OfTypeFilter<TSource, TTarget>(
    val type: KClass<*>,
    val filter: Filter<TTarget>
) : Filter<TSource>