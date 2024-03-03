package de.lise.fluxflow.mongo.query.sort

import de.lise.fluxflow.query.sort.Direction

data class MongoDefaultSort<TModel>(
    private val direction: org.springframework.data.domain.Sort.Direction
) : MongoSort<TModel> {
    constructor(direction: Direction) : this(
        MongoSort.toSpringDirection(direction)
    )

    override fun apply(path: String): org.springframework.data.domain.Sort {
        return org.springframework.data.domain.Sort.by(direction, path)
    }
}