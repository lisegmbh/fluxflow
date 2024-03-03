package de.lise.fluxflow.query.inmemory.sort

import de.lise.fluxflow.query.sort.Direction
import de.lise.fluxflow.query.sort.IncomparableElementsException

data class InMemoryDefaultSort<TModel>(
    val direction: Direction
) : InMemorySort<TModel> {
    @Suppress("UNCHECKED_CAST")
    override fun toComparator(): Comparator<TModel> {
        val baseComparator = Comparator<TModel> { a, b ->
            try {
                (a as Comparable<TModel>).compareTo(b as TModel)
            } catch (e: ClassCastException) {
                throw IncomparableElementsException(
                    a,
                    b,
                    e
                )
            }
        }
        return when (direction) {
            Direction.Ascending -> baseComparator
            Direction.Descending -> baseComparator.reversed()
        }
    }
}