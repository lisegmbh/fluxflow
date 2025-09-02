package de.lise.fluxflow.query.filter

data class InFilter<TValueType>(
    val anyOfValues: Set<TValueType>,
) : Filter<TValueType>