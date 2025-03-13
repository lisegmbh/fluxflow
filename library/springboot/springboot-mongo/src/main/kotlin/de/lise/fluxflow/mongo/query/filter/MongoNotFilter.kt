package de.lise.fluxflow.mongo.query.filter

import org.springframework.data.mongodb.core.query.Criteria

data class MongoNotFilter<TModel>(
    private val filter: MongoFilter<TModel>
) : MongoFilter<TModel> {
    override fun apply(path: String): Criteria {
        // Instead of using .not(), which only negates the immediately following operator,
        // we wrap the inner filter's criteria in a $nor operator. This ensures that the entire
        // composite filter (all its conditions) is effectively negated.
        return Criteria().norOperator(filter.apply(path))
    }
}
