package de.lise.fluxflow.query.inmemory.filter

import kotlin.reflect.KProperty1

class InMemoryElemMatchFilter<TObject, TProperty>(
    private val property: KProperty1<TObject, TProperty>,
    private val filterForProperty: InMemoryFilter<TProperty>
) : InMemoryFilter<Collection<TObject>> {
    override fun toPredicate(): InMemoryPredicate<Collection<TObject>> {
        val propertyPredicate = filterForProperty.toPredicate()
        return InMemoryPredicate { collection ->
            collection.any { value ->
                propertyPredicate.test(property.get(value))
            }
        }
    }
}
