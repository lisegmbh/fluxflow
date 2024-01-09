package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.AnyOfFilter

class InMemoryAnyOfFilter<TModel>(
    private val anyOfValues: List<TModel>,
) : InMemoryFilter<TModel> {
    constructor(anyOfFilter: AnyOfFilter<TModel>) : this(
        anyOfFilter.anyOfValues
    )

    override fun toPredicate(): InMemoryPredicate<TModel> {
        return InMemoryPredicate { value ->
            anyOfValues.contains(value)
        }
    }
}