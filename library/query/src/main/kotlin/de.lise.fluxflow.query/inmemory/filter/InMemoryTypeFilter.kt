package de.lise.fluxflow.test.persistence.query.filter

import de.lise.fluxflow.query.inmemory.filter.InMemoryFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate
import kotlin.reflect.KClass

data class InMemoryTypeFilter<T>(
    private val type: KClass<*>
) : InMemoryFilter<T> {
    override fun toPredicate(): InMemoryPredicate<T> {
        return InMemoryPredicate {
            type.isInstance(it)
        }
    }
}