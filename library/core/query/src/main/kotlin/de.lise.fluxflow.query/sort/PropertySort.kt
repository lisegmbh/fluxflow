package de.lise.fluxflow.query.sort

import kotlin.reflect.KProperty1

data class PropertySort<TObject, TProperty>(
    val property: KProperty1<TObject, TProperty>,
    val sortForProperty: Sort<TProperty>
) : Sort<TObject> {
    override val direction: Direction
        get() = sortForProperty.direction
}