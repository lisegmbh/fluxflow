package de.lise.fluxflow.mongo.query

import de.lise.fluxflow.mongo.query.filter.DocumentFilter
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.support.PageableExecutionUtils
import kotlin.reflect.KClass

typealias MongoQuery = org.springframework.data.mongodb.core.query.Query

fun List<Sort>.toSort(): Sort {
    return when (size) {
        0 -> Sort.unsorted()
        1 -> first()
        else -> reduce { accumulator, next -> accumulator.and(next) }
    }
}

fun <TModel : Any, TFilter : DocumentFilter<TModel>, TQuery : Query<TFilter, Sort>> MongoTemplate.queryPageable(
    query: TQuery,
    type: KClass<TModel>
): Page<TModel> {
    val criteria = query.filter?.toCriteria() ?: Criteria()
    val sort = query.sort.toSort()
    val page = query.page?.let {
        PageRequest.of(it.pageIndex, it.pageSize)
    } ?: Pageable.unpaged()

    val elements = find(
        MongoQuery.query(criteria).with(sort).with(page),
        type.java
    )

    val pageRequest = query.page
        ?: return Page.unpaged(elements) // If the original query was unpaged, we can return the elements right away

    val mongoPage = PageableExecutionUtils.getPage(
        elements,
        page
    ) {
        count(
            MongoQuery.query(criteria).with(sort),
            type.java
        )
    }

    return mongoPage.let {
        Page(
            items = it.content,
            pageSize = pageRequest.pageSize,
            pageIndex = it.pageable.pageNumber,
            totalPages = it.totalPages,
            totalItems = it.totalElements,
            isFirstPage = it.isFirst,
            isLastPage = it.isLast
        )
    }
}