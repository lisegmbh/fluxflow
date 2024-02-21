package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.InFilter

class InMemoryAnyOfFilter<TModel>(
    private val anyOfValues: List<TModel>,
) : InMemoryFilter<TModel> {
    constructor(inFilter: InFilter<TModel>) : this(
        inFilter.anyOfValues
    )

    override fun toPredicate(): InMemoryPredicate<TModel> {
        return InMemoryPredicate { value ->
            anyOfValues.contains(value)
        }
    }
}