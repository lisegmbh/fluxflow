package de.lise.fluxflow.mongo.query.filter

import org.springframework.data.mongodb.core.query.Criteria

data class MongoOrFilter<TModel>(
    private val filters: List<MongoFilter<TModel>>
) : MongoFilter<TModel> {
    override fun apply(path: String): Criteria {
        return Criteria().orOperator(
            filters.map {
                it.apply(path)
            }
        )
    }

}