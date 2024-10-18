package de.lise.fluxflow.query.filter

data class DoesNotContainElementFilter<TCollection : Collection<TModel>, TModel>(
    val value: TModel
) : Filter<TCollection>
