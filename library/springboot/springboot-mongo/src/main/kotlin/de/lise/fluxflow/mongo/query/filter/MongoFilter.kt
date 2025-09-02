package de.lise.fluxflow.mongo.query.filter

import de.lise.fluxflow.query.filter.*
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KProperty1

fun interface MongoFilter<TModel> {
    fun apply(path: String): Criteria

    companion object {
        const val IgnoreStringCasingOption = "i"


        @Suppress("UNCHECKED_CAST")
        fun <TModel> fromDomainFilter(filter: Filter<TModel>): MongoFilter<TModel> {
            return when (filter) {
                is EqualFilter -> MongoEqualFilter(filter)
                is PropertyFilter<*, *> -> MongoPropertyFilter(
                    filter.property as KProperty1<TModel, Any>,
                    fromDomainFilter(filter.filterForProperty) as MongoFilter<Any>
                )
                // Use normal property filter for nullable properties cause mongo handles null values
                is NullablePropertyFilter<*, *> -> MongoPropertyFilter(
                    filter.property as KProperty1<TModel, Any>,
                    fromDomainFilter(filter.filterForProperty) as MongoFilter<Any>
                )

                is MapValueFilter<*, *> -> MongoMapValueFilter(
                    filter.key,
                    fromDomainFilter(filter.filterForValue)
                ) as MongoFilter<TModel>

                is OrFilter<TModel> -> MongoOrFilter(
                    filter.filters.map { fromDomainFilter(it) }
                )

                is AndFilter<TModel> -> MongoAndFilter(
                    filter.filters.map { fromDomainFilter(it) }
                )
                is NotFilter<TModel> -> MongoNotFilter(fromDomainFilter(filter.filter))

                is EndsWithFilter -> MongoEndsWithFilter(filter) as MongoFilter<TModel>
                is StartsWithFilter -> MongoStartsWithFilter(filter) as MongoFilter<TModel>
                is ContainsFilter -> MongoContainsFilter(filter) as MongoFilter<TModel>
                is ContainsElementFilter<*, *> -> MongoContainsElementFilter(filter) as MongoFilter<TModel>
                is ElemMatchFilter<*, *> -> MongoElemMatchFilter(
                    filter.property as KProperty1<TModel, Any>,
                    fromDomainFilter(filter.filterForProperty) as MongoFilter<Any>
                ) as MongoFilter<TModel>
                is DoesNotContainElementFilter<*, *> -> MongoDoesNotContainElementFilter(filter) as MongoFilter<TModel>
                is InFilter -> MongoInFilter(filter)

                is GreaterThanEqualsFilter -> MongoGreaterThanEqualsFilter(filter.value)
                is LowerThanEqualsFilter -> MongoLowerThanEqualsFilter(filter.value)

                is TypeFilter -> MongoEqualFilter(filter.type.qualifiedName) as MongoFilter<TModel>
                is OfTypeFilter<*, *> -> MongoAndFilter(
                    listOf(
                        MongoTypeFilter(filter.type),
                        fromDomainFilter(filter.filter) as MongoFilter<TModel>,
                    )
                )

                else -> throw UnsupportedFilterException(filter)
            }
        }
    }
}

