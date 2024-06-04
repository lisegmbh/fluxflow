package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.InFilter

class InMemoryInFilter<TModel>(
    private val values: Set<TModel>,
) : InMemoryFilter<TModel> {
    constructor(inFilter: InFilter<TModel>) : this(
        inFilter.anyOfValues
    )

    override fun toPredicate(): InMemoryPredicate<TModel> {
        return InMemoryPredicate { value ->
            values.contains(value)
        }
    }
}