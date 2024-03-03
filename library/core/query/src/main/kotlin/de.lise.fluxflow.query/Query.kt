package de.lise.fluxflow.query

import de.lise.fluxflow.query.pagination.PaginationRequest

open class Query<TFilter, TSort>(
    val filter: TFilter?,
    val sort: List<TSort>,
    val page: PaginationRequest?
) {
    fun withFilter(filter: TFilter): Query<TFilter, TSort> {
        return Query(
            filter,
            sort,
            page
        )
    }

    fun mapFilter(mapping: (currentFilter: TFilter?) -> TFilter?): Query<TFilter, TSort> {
        return Query(
            mapping(filter),
            sort,
            page
        )
    }

    fun withoutFilter(): Query<TFilter, TSort> {
        return Query(
            null,
            sort,
            page
        )
    }

    fun addSort(sort: TSort): Query<TFilter, TSort> {
        return Query(
            filter,
            this.sort + sort,
            page
        )
    }

    fun withSort(sort: List<TSort>): Query<TFilter, TSort> {
        return Query(
            filter,
            sort,
            page
        )
    }

    fun withPage(pageRequest: PaginationRequest): Query<TFilter, TSort> {
        return Query(
            filter,
            sort,
            pageRequest
        )
    }

    fun withPage(
        pageIndex: Int,
        pageSize: Int
    ): Query<TFilter, TSort> {
        return withPage(
            PaginationRequest(
                pageIndex,
                pageSize
            )
        )
    }

    fun withoutPage(): Query<TFilter, TSort> {
        return Query(
            filter,
            sort,
            null
        )
    }

    companion object {
        fun <TFilter, TSort> withFilter(filter: TFilter): Query<TFilter, TSort> {
            return Query(
                filter,
                emptyList(),
                null
            )
        }

        fun <TFilter, TSort> empty(): Query<TFilter, TSort> {
            return Query(
                null,
                emptyList(),
                null
            )
        }
    }
}