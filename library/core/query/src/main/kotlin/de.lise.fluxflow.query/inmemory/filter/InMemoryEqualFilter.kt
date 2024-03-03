package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.EqualFilter

class InMemoryEqualFilter<TModel>(
    private val expectedValue: TModel
) : InMemoryFilter<TModel> {
    constructor(filter: EqualFilter<TModel>) : this(
        filter.expectedValue
    )

    override fun toPredicate(): InMemoryPredicate<TModel> {
        return InMemoryPredicate {
            it == expectedValue
        }
    }
}