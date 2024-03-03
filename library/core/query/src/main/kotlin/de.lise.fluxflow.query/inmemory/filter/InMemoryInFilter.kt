package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.InFilter

@Deprecated(
    "This alias will be removed in an upcoming version of FluxFlow. Use InMemoryInFilter<TModel> instead.",
    ReplaceWith("InMemoryInFilter<TModel>")
)
typealias InMemoryAnyOfFilter<TModel> = InMemoryInFilter<TModel>

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