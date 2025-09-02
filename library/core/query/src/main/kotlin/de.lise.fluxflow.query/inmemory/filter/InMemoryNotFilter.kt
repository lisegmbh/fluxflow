package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.NotFilter

data class InMemoryNotFilter<TModel>(
    private val filter: InMemoryFilter<TModel>
) : InMemoryFilter<TModel> {
    constructor(notFilter: NotFilter<TModel>) : this(
        InMemoryFilter.fromDomainFilter(notFilter.filter)
    )

    override fun toPredicate(): InMemoryPredicate<TModel> {
        return InMemoryPredicate { value ->
            !filter.toPredicate().test(value)
        }
    }
}
