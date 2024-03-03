package de.lise.fluxflow.mongo.query

import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.Page
import org.springframework.data.domain.Sort

fun interface QueryableRepository<TModel, TFilter> {
    fun findAll(query: Query<TFilter, Sort>): Page<TModel>
}