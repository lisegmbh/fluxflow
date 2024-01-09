package de.lise.fluxflow.mongo.query

import de.lise.fluxflow.mongo.query.filter.DocumentFilter
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.Page
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import kotlin.reflect.KClass

open class QueryableRepositoryImpl<TModel : Any, TFilter : DocumentFilter<TModel>>(
    private val template: MongoTemplate,
    private val modelClass: KClass<TModel>,
) : QueryableRepository<TModel, TFilter> {

    override fun findAll(query: Query<TFilter, Sort>): Page<TModel> {
        return template.queryPageable(
            query,
            modelClass
        )
    }
}