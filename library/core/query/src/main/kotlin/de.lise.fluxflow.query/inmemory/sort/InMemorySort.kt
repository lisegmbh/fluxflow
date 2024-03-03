package de.lise.fluxflow.query.inmemory.sort

import de.lise.fluxflow.query.sort.DefaultSort
import de.lise.fluxflow.query.sort.PropertySort
import de.lise.fluxflow.query.sort.Sort
import de.lise.fluxflow.query.sort.UnsupportedSortException

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

                else -> throw UnsupportedSortException(sort)
            }
        }
    }
}