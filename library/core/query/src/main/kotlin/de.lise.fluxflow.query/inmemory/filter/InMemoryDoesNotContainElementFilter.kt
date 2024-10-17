package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.DoesNotContainElementFilter

class InMemoryDoesNotContainElementFilter<TCollection : Collection<TModel>, TModel>(
    private val element: TModel,
) : InMemoryFilter<TCollection> {
    constructor(doesNotContainElementFilter: DoesNotContainElementFilter<TCollection, TModel>) : this(
        doesNotContainElementFilter.value
    )

    override fun toPredicate(): InMemoryPredicate<TCollection> {
        return InMemoryPredicate {
            !it.contains(element)
        }
    }
}
