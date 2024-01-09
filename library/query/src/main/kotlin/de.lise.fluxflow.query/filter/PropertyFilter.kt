package de.lise.fluxflow.query.filter

import kotlin.reflect.KProperty1

data class PropertyFilter<TObject, TProperty>(
    val property: KProperty1<TObject, TProperty>,
    val filterForProperty: Filter<TProperty>
) : Filter<TObject>