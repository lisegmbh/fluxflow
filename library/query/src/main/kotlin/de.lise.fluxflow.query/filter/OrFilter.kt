package de.lise.fluxflow.query.filter

data class OrFilter<in TModel>(
    val filters: List<Filter<TModel>>
) : Filter<TModel> {
    constructor(vararg filters: Filter<TModel>) : this(filters.toList())
}