package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.EqualFilter
import org.springframework.data.mongodb.core.query.Criteria

data class MongoEqualFilter<TModel>(
    private val expectedValue: TModel
) : MongoFilter<TModel> {
    constructor(filter: EqualFilter<TModel>) : this(filter.expectedValue)

    override fun apply(path: String): Criteria {
        return Criteria.where(path).`is`(expectedValue)
    }
}