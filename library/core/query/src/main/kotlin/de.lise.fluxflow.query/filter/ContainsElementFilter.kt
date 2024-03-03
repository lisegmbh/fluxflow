package de.lise.fluxflow.query.filter

data class ContainsElementFilter<TCollection : Collection<TModel>, TModel>(
    val value: TModel
) : Filter<TCollection>
