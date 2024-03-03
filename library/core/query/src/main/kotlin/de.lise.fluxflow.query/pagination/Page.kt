package de.lise.fluxflow.query.pagination

data class Page<T>(
    val items: List<T>,
    val pageSize: Int,
    val pageIndex: Int,
    val totalPages: Int,
    val totalItems: Long,
    val isFirstPage: Boolean,
    val isLastPage: Boolean
) {
    val count: Int
        get() = items.size

    fun <TTarget> map(
        mapper: (element: T) -> TTarget
    ): Page<TTarget> {
        return Page(
            items.map(mapper),
            pageSize,
            pageIndex,
            totalPages,
            totalItems,
            isFirstPage,
            isLastPage
        )
    }

    companion object {
        fun <T> unpaged(items: List<T>): Page<T> {
            return Page(
                items,
                items.size,
                0,
                1,
                items.size.toLong(),
                isFirstPage = true,
                isLastPage = true
            )
        }

        fun <T> fromPartialResult(
            pageIndex: Int,
            requestedPageSize: Int,
            itemsOnPage: List<T>,
            allItems: List<T>
        ): Page<T> {
            var totalPages = allItems.size / requestedPageSize
            if (allItems.size % requestedPageSize > 0) {
                // if there is a remainder, add one page
                totalPages += 1
            }

            return Page(
                items = itemsOnPage,
                pageSize = itemsOnPage.size,
                pageIndex = pageIndex,
                totalPages = totalPages,
                totalItems = allItems.size.toLong(),
                isFirstPage = pageIndex == 0,
                isLastPage = pageIndex == totalPages - 1
            )
        }

        fun <T> fromResult(
            allItems: List<T>,
            page: PaginationRequest?
        ): Page<T> {
            if (page == null) {
                return unpaged(allItems)
            }

            val numberOfItemsBeforeCurrentPage = page.pageIndex * page.pageSize
            val itemsOnPage = allItems.drop(numberOfItemsBeforeCurrentPage).take(page.pageSize)

            return fromPartialResult(
                page.pageIndex,
                page.pageSize,
                itemsOnPage,
                allItems
            )
        }
    }
}