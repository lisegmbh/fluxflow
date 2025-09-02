package de.lise.fluxflow.query.inmemory.sort

import kotlin.reflect.KProperty1

data class InMemoryPropertySort<TObject, TProperty>(
    private val property: KProperty1<TObject, TProperty>,
    private val sortForProperty: InMemorySort<TProperty>
) : InMemorySort<TObject> {

    override fun toComparator(): Comparator<TObject> {
        val propertyComparator = sortForProperty.toComparator()

        return Comparator { a, b ->
            val propertyValueA = property.get(a)
            val propertyValueB = property.get(b)

            when {
                propertyValueA == null && propertyValueB == null -> 0 // If both values are null, they are equal.
                propertyValueA == null -> -1 // If first value is null, it is less than the second.
                propertyValueB == null -> 1 // If second value is null, it is greater than the first.

                // If neither value is null, compare them using the property comparator.
                else -> propertyComparator.compare(propertyValueA, propertyValueB)
            }
        }
    }
}
