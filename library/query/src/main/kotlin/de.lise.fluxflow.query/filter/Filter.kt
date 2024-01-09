package de.lise.fluxflow.query.filter

import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface Filter<in TModel> {
    fun <TOther : TModel> and(other: Filter<TOther>): Filter<TOther> {
        return AndFilter(
            listOf(
                this,
                other
            )
        )
    }

    fun <TOther : TModel> or(other: Filter<TOther>): Filter<TOther> {
        return OrFilter(
            this,
            other
        )
    }

    companion object {
        @JvmStatic
        fun <TModel> and(
            filters: List<Filter<TModel>>
        ): Filter<TModel> {
            return AndFilter(filters)
        }

        @JvmStatic
        fun <TModel> and(
            vararg filters: Filter<TModel>
        ): Filter<TModel> {
            return AndFilter(filters.toList())
        }

        @JvmStatic
        fun <TModel> or(
            filters: List<Filter<TModel>>
        ): Filter<TModel> {
            return OrFilter(filters)
        }

        @JvmStatic
        fun <TModel> or(
            vararg filters: Filter<TModel>
        ): Filter<TModel> {
            return OrFilter(filters.toList())
        }

        @JvmStatic
        fun <TModel> eq(
            value: TModel
        ): Filter<TModel> {
            return EqualFilter(value)
        }

        @JvmStatic
        fun startsWith(
            value: String,
            ignoreCasing: Boolean = true
        ): Filter<String> {
            return StartsWithFilter(
                value,
                ignoreCasing
            )
        }

        @JvmStatic
        fun endsWith(
            value: String,
            ignoreCasing: Boolean = true
        ): Filter<String> {
            return EndsWithFilter(value, ignoreCasing)
        }

        @JvmStatic
        fun contains(
            value: String,
            ignoreCasing: Boolean = true
        ): Filter<String> {
            return ContainsFilter(value, ignoreCasing)
        }

        @JvmStatic
        fun <TCollection : Collection<TModel>, TModel> containsElement(
            value: TModel
        ): Filter<TCollection> {
            return ContainsElementFilter(value)
        }

        @JvmStatic
        fun gte(
            value: Number,
        ): Filter<Number> {
            return GreaterThanEqualsFilter(value)
        }

        @JvmStatic
        fun lte(
            value: Number,
        ): Filter<Number> {
            return LowerThanEqualsFilter(value)
        }

        @JvmStatic
        fun gte(
            value: Instant,
        ): Filter<Instant> {
            return GreaterThanEqualsFilter(value)
        }

        @JvmStatic
        fun lte(
            value: Instant,
        ): Filter<Instant> {
            return LowerThanEqualsFilter(value)
        }

        @JvmStatic
        fun <TModel> anyOf(
            value: List<TModel>,
        ): Filter<TModel> {
            return AnyOfFilter(value)
        }

        @JvmStatic
        fun <TKey, TValue> mapValue(
            key: TKey,
            value: Filter<TValue>
        ): Filter<Map<TKey, TValue>> {
            return MapValueFilter(key, value)
        }

        @JvmStatic
        fun <TObject, TProperty> property(
            property: KProperty1<TObject, TProperty>,
            filter: Filter<TProperty>
        ): Filter<TObject> {
            return PropertyFilter(
                property,
                filter
            )
        }

        @JvmStatic
        fun <TObject, TProperty> nullableProperty(
            property: KProperty1<TObject, TProperty?>,
            filter: Filter<TProperty>
        ): Filter<TObject> {
            return PropertyFilter(
                property as KProperty1<TObject, TProperty>,
                filter
            )
        }

        @JvmStatic
        inline fun <reified TModel> ofType(): TypeFilter {
            return TypeFilter(TModel::class)
        }

        @JvmStatic
        inline fun <TSource, reified TTarget> ofType(
            filter: Filter<TTarget>,
        ): Filter<TSource> {
            return OfTypeFilter(
                TTarget::class,
                filter,
            )
        }

        @JvmStatic
        fun ofType(modelType: KClass<*>): TypeFilter {
            return TypeFilter(modelType)
        }
    }
}