package de.lise.fluxflow.mongo.query.filter

import org.springframework.data.mongodb.core.query.Criteria

class MongoGreaterThanEqualsFilter<TModel>(
    private val value: TModel,
) : MongoFilter<TModel> {
    override fun apply(path: String): Criteria {
        return Criteria.where(path).gte(value)
    }
}
