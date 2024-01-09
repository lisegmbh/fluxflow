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

            propertyComparator.compare(propertyValueA, propertyValueB)
        }
    }
}
