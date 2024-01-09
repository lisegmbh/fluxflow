package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.OrFilter

data class InMemoryOrFilter<TModel>(
    private val filters: List<InMemoryFilter<TModel>>
) : InMemoryFilter<TModel> {
    constructor(orFilter: OrFilter<TModel>) : this(orFilter.filters.map {
        InMemoryFilter.fromDomainFilter(it)
    })

    override fun toPredicate(): InMemoryPredicate<TModel> {
        val predicates = filters.map {
            it.toPredicate()
        }
        return InMemoryPredicate { value ->
            predicates.any { predicate ->
                predicate.test(value)
            }
        }
    }
}