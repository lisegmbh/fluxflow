package de.fluxflow.flowquery.repository

import de.fluxflow.flowquery.query.Query
import de.lise.fluxflow.query.pagination.Page

interface NonProjectingRepository<TRoot> {
    fun find(query: Query<TRoot, TRoot>): Page<TRoot>
    fun find(
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TRoot>
    ): Page<TRoot> {
        return find(
            builder(Query.Companion.of())
        )
    }

    fun findFirst(query: Query<TRoot, TRoot>): TRoot
    fun findFirst(
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TRoot>
    ): TRoot {
        return findFirst(
            builder(Query.Companion.of())
        )
    }


    fun findFirstOrNull(query: Query<TRoot, TRoot>): TRoot?
    fun findFirstOrNull(
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TRoot>
    ): TRoot? {
        return findFirstOrNull(
            builder(Query.Companion.of())
        )
    }

    fun findSingle(query: Query<TRoot, TRoot>): TRoot
    fun findSingle(
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TRoot>
    ): TRoot {
        return findSingle(
            builder(Query.Companion.of())
        )
    }


    fun findSingleOrNull(query: Query<TRoot, TRoot>): TRoot?
    fun findSingleOrNull(
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TRoot>
    ): TRoot? {
        return findSingleOrNull(
            builder(Query.Companion.of())
        )
    }
}