package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.InFilter
import org.springframework.data.mongodb.core.query.Criteria

@Deprecated(
    "This alias will be removed in an upcoming version of FluxFlow. Use MongoInFilter<TModel> instead.",
    ReplaceWith("MongoInFilter<TModel>")
)
typealias MongoAnyOfFilter<TModel> = MongoInFilter<TModel>

data class MongoInFilter<TModel>(
    private val values: List<TModel>
) : MongoFilter<TModel> {
    constructor(inFilter: InFilter<TModel>) : this(
        inFilter.anyOfValues
    )

    override fun apply(path: String): Criteria {
        return Criteria.where(path).`in`(values)
    }
}
