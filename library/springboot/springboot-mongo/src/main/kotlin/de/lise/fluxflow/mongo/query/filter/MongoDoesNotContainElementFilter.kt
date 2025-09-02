package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.DoesNotContainElementFilter
import org.springframework.data.mongodb.core.query.Criteria

class MongoDoesNotContainElementFilter<TCollection : Collection<TModel>, TModel>(
    private val element: TModel,
) : MongoFilter<TCollection> {
    constructor(doesNotContainElementFilter: DoesNotContainElementFilter<TCollection, TModel>) : this(
        doesNotContainElementFilter.value
    )

    override fun apply(path: String): Criteria {
        return Criteria.where(path).nin(element)
    }
}
