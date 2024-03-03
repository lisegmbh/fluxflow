package de.lise.fluxflow.query.filter

data class GreaterThanEqualsFilter<TModel>(
    val value: TModel,
) : ValueFilter<TModel>

