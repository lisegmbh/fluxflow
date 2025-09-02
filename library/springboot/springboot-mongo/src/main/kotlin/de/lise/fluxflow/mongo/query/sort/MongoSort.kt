package de.lise.fluxflow.mongo.query.sort

import de.lise.fluxflow.query.sort.*

interface MongoSort<TModel> {
    fun apply(path: String): org.springframework.data.domain.Sort

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <TModel> toMongoSort(sort: Sort<TModel>): MongoSort<TModel> {
            return when (sort) {
                is DefaultSort -> MongoDefaultSort(sort.direction)
                is PropertySort<TModel, *> -> MongoPropertySort(
                    sort.property,
                    toMongoSort(sort.sortForProperty) as MongoSort<Any?>
                )
                is OfTypeSort<TModel, *> -> toMongoSort(sort.sort) as MongoSort<TModel>
                else -> throw UnsupportedSortException(sort)
            }
        }

        fun toSpringDirection(
            direction: Direction
        ): org.springframework.data.domain.Sort.Direction {
            return when (direction) {
                Direction.Ascending -> org.springframework.data.domain.Sort.Direction.ASC
                Direction.Descending -> org.springframework.data.domain.Sort.Direction.DESC
            }
        }
    }
}

