package de.lise.fluxflow.query.inmemory.filter

import kotlin.reflect.KProperty1

data class InMemoryNullablePropertyFilter<TObject, TProperty>(
    private val property: KProperty1<TObject, TProperty?>,
    private val filterForValue: InMemoryFilter<TProperty>
) : InMemoryFilter<TObject> {
    override fun toPredicate(): InMemoryPredicate<TObject> {
        val valuePredicate = filterForValue.toPredicate()

        return InMemoryPredicate {
            val value = property.get(it) ?: return@InMemoryPredicate false

            valuePredicate.test(value)
        }
    }
}