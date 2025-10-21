package de.fluxflow.flowquery.repository

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.FlowQueryBuilder
import de.lise.fluxflow.query.pagination.Page

interface NonProjectingRepository<TRoot> {
    fun find(query: FlowQuery<TRoot, TRoot>): Page<TRoot>
    fun find(
        builder: FlowQueryBuilder<TRoot, TRoot>
    ): Page<TRoot> {
        return find(
            builder(FlowQuery.Companion.of())
        )
    }

    fun findFirst(query: FlowQuery<TRoot, TRoot>): TRoot
    fun findFirst(
        builder: FlowQueryBuilder<TRoot, TRoot>,
    ): TRoot {
        return findFirst(
            builder(FlowQuery.Companion.of())
        )
    }


    fun findFirstOrNull(query: FlowQuery<TRoot, TRoot>): TRoot?
    fun findFirstOrNull(
        builder: FlowQueryBuilder<TRoot, TRoot>
    ): TRoot? {
        return findFirstOrNull(
            builder(FlowQuery.Companion.of())
        )
    }

    fun findSingle(query: FlowQuery<TRoot, TRoot>): TRoot
    fun findSingle(
        builder: FlowQueryBuilder<TRoot, TRoot>
    ): TRoot {
        return findSingle(
            builder(FlowQuery.Companion.of())
        )
    }


    fun findSingleOrNull(query: FlowQuery<TRoot, TRoot>): TRoot?
    fun findSingleOrNull(
        builder: FlowQueryBuilder<TRoot, TRoot>
    ): TRoot? {
        return findSingleOrNull(
            builder(FlowQuery.Companion.of())
        )
    }
}