package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.ContainsElementFilter
import org.springframework.data.mongodb.core.query.Criteria

class MongoContainsElementFilter<TCollection : Collection<TModel>, TModel>(
    private val element: TModel,
) : MongoFilter<TCollection> {
    constructor(containsElementFilter: ContainsElementFilter<TCollection, TModel>) : this(
        containsElementFilter.value
    )

    override fun apply(path: String): Criteria {
        return Criteria.where(path).`in`(element)
    }
}
