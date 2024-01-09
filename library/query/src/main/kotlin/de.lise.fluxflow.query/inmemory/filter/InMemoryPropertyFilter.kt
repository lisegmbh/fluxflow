package de.lise.fluxflow.query.inmemory.filter

import kotlin.reflect.KProperty1

data class InMemoryPropertyFilter<TObject, TProperty>(
    private val property: KProperty1<TObject, TProperty>,
    private val filterForValue: InMemoryFilter<TProperty>
) : InMemoryFilter<TObject> {
    override fun toPredicate(): InMemoryPredicate<TObject> {
        val valuePredicate = filterForValue.toPredicate()
        return InMemoryPredicate {
            valuePredicate.test(property.get(it))
        }
    }
}