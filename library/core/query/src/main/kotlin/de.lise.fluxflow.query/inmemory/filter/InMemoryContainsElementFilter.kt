package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.ContainsElementFilter

class InMemoryContainsElementFilter<TCollection : Collection<TModel>, TModel>(
    private val element: TModel,
) : InMemoryFilter<TCollection> {
    constructor(containsElementFilter: ContainsElementFilter<TCollection, TModel>) : this(
        containsElementFilter.value
    )

    override fun toPredicate(): InMemoryPredicate<TCollection> {
        return InMemoryPredicate {
            it.contains(element)
        }
    }
}
