package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.AndFilter

data class InMemoryAndFilter<TModel>(
    private val filters: List<InMemoryFilter<TModel>>
) : InMemoryFilter<TModel> {
    constructor(andFilter: AndFilter<TModel>) : this(andFilter.filters.map {
        InMemoryFilter.fromDomainFilter(andFilter)
    })

    override fun toPredicate(): InMemoryPredicate<TModel> {
        val predicates = filters.map { it.toPredicate() }
        return InMemoryPredicate { value ->
            predicates.all { predicate ->
                predicate.test(value)
            }
        }
    }
}