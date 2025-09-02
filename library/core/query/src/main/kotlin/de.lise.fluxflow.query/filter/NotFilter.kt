package de.lise.fluxflow.query.filter

data class NotFilter<TModel>(
    val filter: Filter<TModel>
) : Filter<TModel>
