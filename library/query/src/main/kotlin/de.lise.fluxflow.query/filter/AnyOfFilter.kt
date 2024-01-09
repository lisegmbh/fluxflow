package de.lise.fluxflow.query.filter

data class AnyOfFilter<TValueType>(
    val anyOfValues: List<TValueType>,
) : Filter<TValueType>