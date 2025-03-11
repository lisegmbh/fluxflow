package de.lise.fluxflow.mongo.query.filter

import org.springframework.data.mongodb.core.query.Criteria

data class MongoNotFilter<TModel>(
    private val filter: MongoFilter<TModel>
) : MongoFilter<TModel> {
    override fun apply(path: String): Criteria {
        // This wraps the inner filter's criteria in a $nor operator,
        // effectively negating it.
        return Criteria().norOperator(filter.apply(path))
    }
}
