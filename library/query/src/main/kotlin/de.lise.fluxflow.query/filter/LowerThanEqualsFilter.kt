package de.lise.fluxflow.query.filter

data class LowerThanEqualsFilter<TModel>(
    val value: TModel,
) : ValueFilter<TModel>

