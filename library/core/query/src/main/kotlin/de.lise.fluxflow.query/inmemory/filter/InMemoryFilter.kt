package de.lise.fluxflow.query.inmemory.filter

import de.lise.fluxflow.query.filter.*
import de.lise.fluxflow.test.persistence.query.filter.InMemoryTypeFilter
import kotlin.reflect.KProperty1

fun interface InMemoryFilter<TModel> {
    fun toPredicate(): InMemoryPredicate<TModel>

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <TModel> fromDomainFilter(
            filter: Filter<TModel>
        ): InMemoryFilter<TModel> {
            return when (filter) {
                is EqualFilter -> InMemoryEqualFilter(filter)
                is PropertyFilter<*, *> -> InMemoryPropertyFilter(
                    filter.property as KProperty1<TModel, Any>,
                    fromDomainFilter(filter.filterForProperty) as InMemoryFilter<Any>
                )

                is MapValueFilter<*, *> -> InMemoryMapValueFilter(
                    filter.key,
                    fromDomainFilter(filter.filterForValue)
                ) as InMemoryFilter<TModel>

                is StartsWithFilter -> InMemoryStartsWithFilter(filter) as InMemoryFilter<TModel>
                is EndsWithFilter -> InMemoryEndsWithFilter(filter) as InMemoryFilter<TModel>
                is ContainsFilter -> InMemoryContainsFilter(filter) as InMemoryFilter<TModel>
                is ContainsElementFilter<*, *> -> InMemoryContainsElementFilter(filter) as InMemoryFilter<TModel>
                is InFilter -> InMemoryInFilter(filter)

                is GreaterThanEqualsFilter -> InMemoryGreaterThanEqualsFilter(filter)
                is LowerThanEqualsFilter -> InMemoryLowerThanEqualsFilter(filter)

                is OrFilter -> InMemoryOrFilter(filter)
                is AndFilter -> InMemoryAndFilter(filter)
                is TypeFilter -> InMemoryTypeFilter(filter.type)
                is OfTypeFilter<*, *> -> InMemoryAndFilter(
                    listOf(
                        InMemoryTypeFilter<TModel>(filter.type) as InMemoryFilter<TModel>,
                        fromDomainFilter(filter.filter) as InMemoryFilter<TModel>
                    )
                )

                else -> throw UnsupportedFilterException(filter)
            }
        }
    }
}

