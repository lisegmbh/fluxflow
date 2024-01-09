package de.lise.fluxflow.query.filter

class MapValueFilter<TKey,TValue>(
    val key: TKey,
    val filterForValue: Filter<TValue>
) : Filter<Map<TKey, TValue>>