package de.lise.fluxflow.query.sort

import kotlin.reflect.KProperty1

interface Sort<TModel> {
    val direction: Direction

    companion object {
        fun <TModel> asc(): Sort<TModel> {
            return DefaultSort(Direction.Ascending)
        }

        fun <TModel> desc(): Sort<TModel> {
            return DefaultSort(Direction.Descending)
        }

        fun <TObject, TProperty> property(
            property: KProperty1<TObject, TProperty>,
            sortForProperty: Sort<TProperty>
        ): Sort<TObject> {
            return PropertySort(property, sortForProperty)
        }
    }
}

