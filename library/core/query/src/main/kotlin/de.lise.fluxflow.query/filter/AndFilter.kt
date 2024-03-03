package de.lise.fluxflow.query.filter

class AndFilter<in TModel>(
    val filters: List<Filter<TModel>>
) : Filter<TModel>

