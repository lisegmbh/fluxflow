package de.lise.fluxflow.mongo.query.filter

import org.springframework.data.mongodb.core.query.Criteria

class MongoMapValueFilter<TKey, TValue>(
    private val key: TKey,
    private val filterForValue: MongoFilter<TValue>
) : MongoFilter<Map<TKey, TValue>> {
    override fun apply(path: String): Criteria {
        return filterForValue.apply(
            "${path}.${key}"
        )
    }
}