package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.AnyOfFilter
import org.springframework.data.mongodb.core.query.Criteria

data class MongoAnyOfFilter<TModel>(
    private val anyOfValues: List<TModel>
) : MongoFilter<TModel> {
    constructor(anyOfFilter: AnyOfFilter<TModel>) : this(
        anyOfFilter.anyOfValues
    )

    override fun apply(path: String): Criteria {
        return Criteria.where(path).`in`(anyOfValues)
    }
}
