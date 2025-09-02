package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.InFilter
import org.springframework.data.mongodb.core.query.Criteria

data class MongoInFilter<TModel>(
    private val values: Set<TModel>
) : MongoFilter<TModel> {
    constructor(inFilter: InFilter<TModel>) : this(
        inFilter.anyOfValues
    )

    override fun apply(path: String): Criteria {
        return Criteria.where(path).`in`(values)
    }
}