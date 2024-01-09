package de.lise.fluxflow.query.filter

class EqualFilter<TValueType>(
    val expectedValue: TValueType
) : ValueFilter<TValueType>