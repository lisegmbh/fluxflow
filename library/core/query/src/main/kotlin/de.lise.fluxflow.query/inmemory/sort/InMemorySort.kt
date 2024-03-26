package de.lise.fluxflow.query.inmemory.sort

import de.lise.fluxflow.query.sort.*

interface InMemorySort<TModel> {
    fun toComparator(): Comparator<TModel>

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <TModel> toTestSort(sort: Sort<TModel>): InMemorySort<TModel> {
            return when (sort) {
                is DefaultSort -> InMemoryDefaultSort(sort.direction)
                is PropertySort<TModel, *> -> InMemoryPropertySort(
                    sort.property,
                    toTestSort(sort.sortForProperty) as InMemorySort<Any?>
                )
                is OfTypeSort<TModel, *> -> toTestSort(sort.sort) as InMemorySort<TModel>
                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}