package de.lise.fluxflow.query.sort

import kotlin.reflect.KProperty1

/**
 * A [Sort] can be used to define the sorting of a query.
 * @param TModel The type of the model to sort.
 */
interface Sort<TModel> {
    /**
     * The direction of the sort.
     */
    val direction: Direction

    companion object {
        /**
         * @return A new [Sort] with the direction [Direction.Ascending].
         */
        fun <TModel> asc(): Sort<TModel> {
            return DefaultSort(Direction.Ascending)
        }

        /**
         * @return A new [Sort] with the direction [Direction.Descending].
         */
        fun <TModel> desc(): Sort<TModel> {
            return DefaultSort(Direction.Descending)
        }

        /**
         * Creates a new [Sort] for a property of a model.
         * @param property The property to sort for.
         * @param sortForProperty The sort for the property.
         * @return A new [Sort] for the property of the model with the given sort.
         */
        fun <TObject, TProperty> property(
            property: KProperty1<TObject, TProperty>,
            sortForProperty: Sort<TProperty>
        ): Sort<TObject> {
            return PropertySort(property, sortForProperty)
        }

        /**
         * Creates a new [Sort] for a nullable property of a model.
         * @param property The property to sort for.
         * @param sortForProperty The sort for the property.
         * @return A new [Sort] for the nullable property of the model with the given sort.
         */
        fun <TObject, TProperty> nullableProperty(
            property: KProperty1<TObject, TProperty?>,
            sortForProperty: Sort<TProperty>
        ): Sort<TObject> {
            return PropertySort(
                property as KProperty1<TObject, TProperty>,
                sortForProperty
            )
        }
    }
}

