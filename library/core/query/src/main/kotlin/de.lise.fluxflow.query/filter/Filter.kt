package de.lise.fluxflow.query.filter

import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * A [Filter] can be used to check if an element of type [TModel] matches a certain condition.
 * @param TModel The type of elements filterable by this filter.
 */
interface Filter<in TModel> {
    /**
     * Returns a new [Filter] that matches if this and the [other] provided filter is matching.
     * @param other The other filter to be AND combined.
     * @return A filter that matches if this and the [other] provided filter is matching.
     */
    fun <TOther : TModel> and(other: Filter<TOther>): Filter<TOther> {
        return AndFilter(
            listOf(
                this,
                other
            )
        )
    }

    /**
     * Returns a new [Filter] that matches if this or the [other] provided filter matches.
     * @param other The other filter to be OR combined.
     * @return A filter that matches if this or the [other] provided filter is matching.
     */
    fun <TOther : TModel> or(other: Filter<TOther>): Filter<TOther> {
        return OrFilter(
            this,
            other
        )
    }

    companion object {
        /**
         * Creates a filter that matches, if every filter provided in [filters] matches.
         * @param filters The filters to be combined using a logical AND operation.
         * @return A filter that returns true for elements, matched by all provided [filters].
         */
        @JvmStatic
        fun <TModel> and(
            filters: List<Filter<TModel>>
        ): Filter<TModel> {
            return AndFilter(filters)
        }

        /**
         * Creates a filter that matches, if every filter provided in [filters] matches.
         * @param filters The filters to be combined using a logical AND operation.
         * @return A filter that returns true for elements, matched by all provided [filters].
         */
        @JvmStatic
        fun <TModel> and(
            vararg filters: Filter<TModel>
        ): Filter<TModel> {
            return AndFilter(filters.toList())
        }

        /**
         * Creates a filter that matches, if any filter provided in [filters] matches.
         * @param filters The filters to be combined using a logical OR operation.
         * @return A filter that returns true for elements, matched by at least one of the provided [filters].
         */
        @JvmStatic
        fun <TModel> or(
            filters: List<Filter<TModel>>
        ): Filter<TModel> {
            return OrFilter(filters)
        }

        /**
         * Creates a filter that matches, if any filter provided in [filters] matches.
         * @param filters The filters to be combined using a logical OR operation.
         * @return A filter that returns true for elements, matched by at least one of the provided [filters].
         */
        @JvmStatic
        fun <TModel> or(
            vararg filters: Filter<TModel>
        ): Filter<TModel> {
            return OrFilter(filters.toList())
        }

        /**
         * Creates a filter that matches, if the provided [filter] does not match.
         * @param filter The filter to be negated.
         * @return A filter that matches if the provided [filter] does not match.
         */
        @JvmStatic
        fun <TModel> not(
            filter: Filter<TModel>
        ): Filter<TModel> {
            return NotFilter(filter)
        }

        /**
         * Creates a filter that matches values equal to the provided [value].
         * @param value The expected value.
         * @return A filter that matches every value, that is equal to the provided [value].
         */
        @JvmStatic
        fun <TModel> eq(
            value: TModel
        ): Filter<TModel> {
            return EqualFilter(value)
        }

        /**
         * Creates a filter that matches strings starting with the provided [prefix].
         * @param prefix The prefix that must be present at the start of strings to be matched.
         * @param ignoreCasing If set to `true`, casing will be ignored when checking for the [prefix]. The default is `true`.
         * @return A filter matching strings starting with the supplied [prefix].
         */
        @JvmStatic
        fun startsWith(
            prefix: String,
            ignoreCasing: Boolean = true
        ): Filter<String> {
            return StartsWithFilter(
                prefix,
                ignoreCasing
            )
        }

        /**
         * Creates a filter that matches strings ending with the provided [suffix].
         * @param suffix The suffix that must be present at the end of strings to be matched.
         * @param ignoreCasing If set to `true`, casing will be ignored when checking for the [suffix]. The default is `true`.
         * @return A filter matching strings ending with the supplied [suffix].
         */
        @JvmStatic
        fun endsWith(
            suffix: String,
            ignoreCasing: Boolean = true
        ): Filter<String> {
            return EndsWithFilter(suffix, ignoreCasing)
        }

        /**
         * Creates a filter that matches strings containing the provided [substring].
         * @param substring The substring to be contained within strings to be matched.
         * @return A filter matching strings containing the supplied [substring].
         */
        @JvmStatic
        fun contains(
            substring: String,
            ignoreCasing: Boolean = true
        ): Filter<String> {
            return ContainsFilter(substring, ignoreCasing)
        }

        /**
         * Creates a filter that matches collections containing the given [element].
         * @param element The element to be present within matching collections.
         * @return A filter matching all collections containing the given [element].
         */
        @JvmStatic
        fun <TCollection : Collection<TModel>, TModel> containsElement(
            element: TModel
        ): Filter<TCollection> {
            return ContainsElementFilter(element)
        }

        /**
         * Creates a filter that matches collections not containing the given [element].
         * @param element The element to be absent within matching collections.
         * @return A filter matching all collections not containing the given [element].
         */
        @JvmStatic
        fun <TCollection : Collection<TModel>, TModel> doesNotContainElement(
            element: TModel
        ): Filter<TCollection> {
            return DoesNotContainElementFilter(element)
        }

        /**
         * Creates a filter that matches numeric values that are greater than or equal to the supplied [value].
         * @param value The value matching elements must be equal to or greater than.
         * @return A filter matching if `actual >= value`.
         */
        @JvmStatic
        fun gte(
            value: Number,
        ): Filter<Number> {
            return GreaterThanEqualsFilter(value)
        }

        /**
         * Creates a filter that matches numeric values that are less than or equal to the supplied [value].
         * @param value The value matching elements must be equal to or less than.
         * @return A filter matching if `actual <= value`.
         */
        @JvmStatic
        fun lte(
            value: Number,
        ): Filter<Number> {
            return LowerThanEqualsFilter(value)
        }

        /**
         * Creates a filter matching instants occurring after or at the same time as [value].
         *
         * This is an alias for [Filter.afterOrEqual].
         * @param value The instant to compare to.
         * @return A filter matching all instants occurring after or at the same time as [value].
         * @see Filter.afterOrEqual
         */
        @JvmStatic
        fun gte(
            value: Instant,
        ): Filter<Instant> {
            return afterOrEqual(value)
        }

        /**
         * Creates a filter matching instants occurring after or at the same time as [instant].
         * @param instant The instant to compare to.
         * @return A filter matching all instants occurring after or at the same time as [instant].
         */
        @JvmStatic
        fun afterOrEqual(
            instant: Instant
        ): Filter<Instant> {
            return GreaterThanEqualsFilter(instant)
        }

        /**
         * Creates a filter matching instants occurring before or at the same time as [value].
         *
         * This is an alias for [Filter.beforeOrEqual]
         * @param value The instant to compare to.
         * @return A filter matching all instants occurring before or at the same time as [value].
         * @see Filter.beforeOrEqual
         */
        @JvmStatic
        fun lte(
            value: Instant,
        ): Filter<Instant> {
            return beforeOrEqual(value)
        }

        /**
         * Creates a filter matching instants occurring before or at the same time as [instant].
         * @param instant The instant to compare to.
         * @return A filter matching all instants occurring before or at the same time as [instant].
         */
        @JvmStatic
        fun beforeOrEqual(
            instant: Instant
        ): Filter<Instant> {
            return LowerThanEqualsFilter(instant)
        }

        /**
         * Creates a filter that matches values that are equal to at least one of the supplied [values].
         *
         * This is an alias for [Filter.in].
         * @param values A list of possible values.
         * @return A filter matching values that are contained within [values].
         * @see [Filter.in]
         */
        @JvmStatic
        fun <TModel> anyOf(
            values: Collection<TModel>,
        ): Filter<TModel> {
            return `in`(values)
        }

        /**
         * Creates a filter that matches values that are equal to at least one of the supplied [values].
         *
         * This is an alias for [Filter.in].
         * @param values A list of possible values.
         * @return A filter matching values that are contained within [values].
         * @see [Filter.in]
         */
        @JvmStatic
        fun <TModel> anyOf(
            values: Set<TModel>
        ): Filter<TModel> {
            return `in`(values)
        }

        /**
         * Creates a filter that matches values that are equal to at least one of the supplied [values].
         * 
         * This is an alias for [Filter.in].
         * @param values A list of possible values.
         * @return A filter matching values that are contained within [values].
         * @see [Filter.in]
         */
        @JvmStatic
        fun <TModel> anyOf(
            vararg values: TModel
        ): Filter<TModel> {
            return `in`(values)
        }

        /**
         * Creates a filter that matches values that are equal to at least one of the supplied [values].
         * @param values A list of possible values.
         * @return A filter matching values that are contained within [values].
         */
        @JvmStatic
        fun <TModel> `in`(
            values: Collection<TModel>
        ): Filter<TModel> {
            // The cast within the `when` block is specified explicitly to avoid mistaking it for an infinite recursion
            @Suppress("USELESS_CAST")
            return when (values) {
                is Set<TModel> -> `in`(values as Set<TModel>)
                else -> `in`(values.toSet())
            }
        }

        /**
         * Creates a filter that matches values that are equal to at least one of the supplied [values].
         * @param values A list of possible values.
         * @return A filter matching values that are contained within [values].
         */
        @JvmStatic
        fun <TModel> `in`(
            values: Set<TModel>
        ): Filter<TModel> {
            return InFilter(values)
        }

        /**
         * Creates a filter that matches values that are equal to at least one of the supplied [values].
         * @param values A list of possible values.
         * @return A filter matching values that are contained within [values].
         */
        @JvmStatic
        fun <TModel> `in`(
            vararg values: TModel
        ): Filter<TModel> {
            return `in`(values.toSet())
        }

        /**
         * Returns a filter
         * that is matching maps containing an entry for [key] associated to a value
         * matching the provided [valueFilter].
         * @param key The key to be present within matching maps.
         * @param valueFilter The value filter to be applied on the map entry's value.
         * @return A filter matching if [valueFilter] matches `actual[key]`.
         */
        @JvmStatic
        fun <TKey, TValue> mapValue(
            key: TKey,
            valueFilter: Filter<TValue>
        ): Filter<Map<TKey, TValue>> {
            return MapValueFilter(key, valueFilter)
        }

        /**
         * Returns a filter that matches objects having the specified [property] matching the supplied [propertyFilter].
         * @param property Specifies the element's property.
         * @param propertyFilter The filter to be applied to the element's [property].
         * @return A filter that is matching for elements if [propertyFilter] matches `actual.property`.
         */
        @JvmStatic
        fun <TObject, TProperty> property(
            property: KProperty1<TObject, TProperty>,
            propertyFilter: Filter<TProperty>
        ): Filter<TObject> {
            return PropertyFilter(
                property,
                propertyFilter
            )
        }

        /**
         * Returns a filter that matches objects having the specified [property] matching the supplied [propertyFilter].
         * @param property Specifies the element's property.
         * @param propertyFilter The filter to be applied to the element's [property].
         * @return A filter that is matching for elements if [propertyFilter] matches `actual.property`.
         */
        @JvmStatic
        fun <TObject, TProperty> nullableProperty(
            property: KProperty1<TObject, TProperty?>,
            propertyFilter: Filter<TProperty>
        ): Filter<TObject> {
            return PropertyFilter(
                property as KProperty1<TObject, TProperty>,
                propertyFilter
            )
        }

        /**
         * Returns a filter that matches elements that are assignable to the specified [TModel].
         * @param TModel The type of element to be matched.
         * @return A filter matching all elements assignable to [TModel].
         */
        @JvmStatic
        inline fun <reified TModel> ofType(): TypeFilter {
            return TypeFilter(TModel::class)
        }

        /**
         * Returns a filter
         * that matches elements that are assignable to the specified
         * [TTarget] and are also matched by the supplied [elementFilter].
         * @param TTarget The type of element to be matched.
         * @param elementFilter The filter that should match.
         * @return A filter matching all elements that are assignable to [TTarget] and are also matched by [elementFilter].
         */
        @JvmStatic
        inline fun <TSource, reified TTarget> ofType(
            elementFilter: Filter<TTarget>,
        ): Filter<TSource> {
            return OfTypeFilter(
                TTarget::class,
                elementFilter,
            )
        }

        /**
         * Returns a filter that matches elements that are assignable to the specified [modelType].
         * @param modelType The type of element to be matched.
         * @return A filter matching all elements assignable to [modelType].
         */
        @JvmStatic
        fun ofType(modelType: KClass<*>): TypeFilter {
            return TypeFilter(modelType)
        }
    }
}