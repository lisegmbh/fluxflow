package de.fluxflow.flowquery.repository

import de.fluxflow.flowquery.query.Query
import kotlin.reflect.KClass

interface ProjectingRepository<TRoot> {
    fun <TResult> find(resultType: Class<TResult>, query: Query<TRoot, TResult>): List<TResult>
    fun <TResult> find(
        resultType: Class<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): List<TResult> {
        return find(
            resultType,
            builder(Query.Companion.of())
        )
    }
    fun <TResult : Any> find(resultType: KClass<TResult>, query: Query<TRoot, TResult>): List<TResult> {
        return find(resultType.java, query)
    }
    fun <TResult : Any> find(
        resultType: KClass<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): List<TResult> {
        return find(
            resultType.java,
            builder
        )
    }

    fun <TResult> findFirst(resultType: Class<TResult>, query: Query<TRoot, TResult>): TResult
    fun <TResult> findFirst(
        resultType: Class<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): TResult {
        return findFirst(
            resultType,
            builder(Query.Companion.of())
        )
    }
    fun <TResult : Any> findFirst(resultType: KClass<TResult>, query: Query<TRoot, TResult>): TResult {
        return findFirst(
            resultType.java,
            query
        )
    }
    fun <TResult: Any> findFirst(
        resultType: KClass<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): TResult {
        return findFirst(
            resultType.java,
            builder
        )
    }

    fun <TResult> findFirstOrNull(resultType: Class<TResult>, query: Query<TRoot, TResult>): TResult?
    fun <TResult> findFirstOrNull(
        resultType: Class<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): TResult? {
        return findFirstOrNull(
            resultType,
            builder(Query.Companion.of())
        )
    }
    fun <TResult: Any> findFirstOrNull(resultType: KClass<TResult>, query: Query<TRoot, TResult>): TResult? {
        return findFirstOrNull(
            resultType.java,
            query
        )
    }
    fun <TResult: Any> findFirstOrNull(
        resultType: KClass<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): TResult? {
        return findFirstOrNull(
            resultType.java,
            builder
        )
    }

    fun <TResult> findSingle(resultType: Class<TResult>, query: Query<TRoot, TResult>): TResult
    fun <TResult> findSingle(
        resultType: Class<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): TResult {
        return findSingle(
            resultType,
            builder(Query.Companion.of())
        )
    }
    fun <TResult: Any> findSingle(resultType: KClass<TResult>, query: Query<TRoot, TResult>): TResult {
        return findSingle(
            resultType.java,
            query
        )
    }
    fun <TResult: Any> findSingle(
        resultType: KClass<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): TResult {
        return findSingle(
            resultType.java,
            builder
        )
    }

    fun <TResult> findSingleOrNull(resultType: Class<TResult>, query: Query<TRoot, TResult>): TResult?
    fun <TResult> findSingleOrNull(
        resultType: Class<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): TResult? {
        return findSingleOrNull(
            resultType,
            builder(Query.Companion.of())
        )
    }
    fun <TResult: Any> findSingleOrNull(resultType: KClass<TResult>, query: Query<TRoot, TResult>): TResult?{
        return findSingleOrNull(
            resultType.java,
            query
        )
    }
    fun <TResult: Any> findSingleOrNull(
        resultType: KClass<TResult>,
        builder: Query<TRoot, TRoot>.() -> Query<TRoot, TResult>
    ): TResult? {
        return findSingleOrNull(
            resultType.java,
            builder
        )
    }
}