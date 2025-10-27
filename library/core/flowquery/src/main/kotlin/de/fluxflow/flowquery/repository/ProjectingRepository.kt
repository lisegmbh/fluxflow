package de.fluxflow.flowquery.repository

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.FlowQueryBuilder
import kotlin.reflect.KClass

/**
 * Defines a repository interface that executes [FlowQuery] objects and returns projected results.
 *
 * The [ProjectingRepository] supports type-safe queries using [FlowQuery],
 * returning data of various projected result types. Queries may be defined directly
 * or constructed via builder functions.
 *
 * @param TRoot the root entity or domain type managed by this repository
 */
interface ProjectingRepository<TRoot> {

    // --------------------------------------------------------------------------------------------
    // find (multiple results)
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns all matching results of the specified type.
     *
     * Example:
     * ```
     * val results = repository.find(UserSummary::class.java,
     *     FlowQuery.of<User>().project { get(User::name) })
     * ```
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param query the query to execute
     * @return a list of results matching the given query
     */
    fun <TResult> find(resultType: Class<TResult>, query: FlowQuery<TRoot, TResult>): List<TResult>

    /**
     * Executes a query built via a builder function and returns all matching results.
     *
     * Example:
     * ```
     * val results = repository.find(UserSummary::class.java) {
     *     it.project { get(User::name) }
     * }
     * ```
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return a list of results matching the built query
     */
    fun <TResult> find(
        resultType: Class<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>,
    ): List<TResult> {
        return find(resultType, builder(FlowQuery.Companion.of()))
    }

    /**
     * Executes the given query and returns all matching results, using a Kotlin [KClass] type reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param query the query to execute
     * @return a list of results matching the given query
     */
    fun <TResult : Any> find(
        resultType: KClass<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): List<TResult> {
        return find(resultType.java, query)
    }

    /**
     * Executes a query built via a builder function, using a Kotlin [KClass] type reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return a list of results matching the built query
     */
    fun <TResult : Any> find(
        resultType: KClass<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>,
    ): List<TResult> {
        return find(resultType.java, builder)
    }


    // --------------------------------------------------------------------------------------------
    // findFirst
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns the first matching result.
     *
     * Example:
     * ```
     * val firstUser = repository.findFirst(User::class.java,
     *     FlowQuery.of<User>().where { get(User::isActive).isEqual(true) })
     * ```
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param query the query to execute
     * @return the first matching result
     * @throws NoSuchElementException if no result is found
     */
    fun <TResult> findFirst(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult

    /**
     * Executes a query built via a builder function and returns the first matching result.
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the first matching result
     * @throws NoSuchElementException if no result is found
     */
    fun <TResult> findFirst(
        resultType: Class<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>
    ): TResult {
        return findFirst(resultType, builder(FlowQuery.Companion.of()))
    }

    /**
     * Executes the given query and returns the first matching result, using a Kotlin [KClass] type reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param query the query to execute
     * @return the first matching result
     * @throws NoSuchElementException if no result is found
     */
    fun <TResult : Any> findFirst(
        resultType: KClass<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult {
        return findFirst(resultType.java, query)
    }

    /**
     * Executes a query built via a builder function and returns the first matching result, using a Kotlin [KClass].
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the first matching result
     * @throws NoSuchElementException if no result is found
     */
    fun <TResult : Any> findFirst(
        resultType: KClass<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>
    ): TResult {
        return findFirst(resultType.java, builder)
    }


    // --------------------------------------------------------------------------------------------
    // findFirstOrNull
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns the first matching result, or `null` if none is found.
     *
     * Example:
     * ```
     * val user = repository.findFirstOrNull(User::class.java,
     *     FlowQuery.of<User>().where { get(User::id).isEqual(42) })
     * ```
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param query the query to execute
     * @return the first matching result, or `null` if none found
     */
    fun <TResult> findFirstOrNull(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult?

    /**
     * Executes a query built via a builder function and returns the first matching result, or `null` if none is found.
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the first matching result, or `null` if none found
     */
    fun <TResult> findFirstOrNull(
        resultType: Class<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>,
    ): TResult? {
        return findFirstOrNull(resultType, builder(FlowQuery.Companion.of()))
    }

    /**
     * Executes the given query and returns the first matching result, or `null` if none is found.
     * Uses a Kotlin [KClass] type reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param query the query to execute
     * @return the first matching result, or `null` if none found
     */
    fun <TResult : Any> findFirstOrNull(
        resultType: KClass<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult? {
        return findFirstOrNull(resultType.java, query)
    }

    /**
     * Executes a query built via a builder function and returns the first matching result, or `null` if none is found.
     * Uses a Kotlin [KClass] type reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the first matching result, or `null` if none found
     */
    fun <TResult : Any> findFirstOrNull(
        resultType: KClass<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>,
    ): TResult? {
        return findFirstOrNull(resultType.java, builder)
    }


    // --------------------------------------------------------------------------------------------
    // findSingle
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns exactly one matching result.
     *
     * Example:
     * ```
     * val user = repository.findSingle(User::class.java,
     *     FlowQuery.of<User>().where { get(User::id).isEqual(42) })
     * ```
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param query the query to execute
     * @return the single matching result
     * @throws NoSuchElementException if no result is found
     * @throws IllegalStateException if more than one result is found
     */
    fun <TResult> findSingle(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult

    /**
     * Executes a query built via a builder function and returns exactly one matching result.
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the single matching result
     * @throws NoSuchElementException if no result is found
     * @throws IllegalStateException if more than one result is found
     */
    fun <TResult> findSingle(
        resultType: Class<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>,
    ): TResult {
        return findSingle(resultType, builder(FlowQuery.Companion.of()))
    }

    /**
     * Executes the given query and returns exactly one matching result, using a Kotlin [KClass] reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param query the query to execute
     * @return the single matching result
     * @throws NoSuchElementException if no result is found
     * @throws IllegalStateException if more than one result is found
     */
    fun <TResult : Any> findSingle(
        resultType: KClass<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult {
        return findSingle(resultType.java, query)
    }

    /**
     * Executes a query built via a builder function and returns exactly one matching result,
     * using a Kotlin [KClass] reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the single matching result
     * @throws NoSuchElementException if no result is found
     * @throws IllegalStateException if more than one result is found
     */
    fun <TResult : Any> findSingle(
        resultType: KClass<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>
    ): TResult {
        return findSingle(resultType.java, builder)
    }


    // --------------------------------------------------------------------------------------------
    // findSingleOrNull
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns a single matching result, or `null` if none is found.
     *
     * Example:
     * ```
     * val user = repository.findSingleOrNull(User::class.java,
     *     FlowQuery.of<User>().where { get(User::email).isEqual("a@b.com") })
     * ```
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param query the query to execute
     * @return the single matching result, or `null` if none found
     * @throws IllegalStateException if more than one result is found
     */
    fun <TResult> findSingleOrNull(
        resultType: Class<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult?

    /**
     * Executes a query built via a builder function and returns a single matching result, or `null` if none is found.
     *
     * @param TResult the result type to project to
     * @param resultType the Java class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the single matching result, or `null` if none found
     * @throws IllegalStateException if more than one result is found
     */
    fun <TResult> findSingleOrNull(
        resultType: Class<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>,
    ): TResult? {
        return findSingleOrNull(resultType, builder(FlowQuery.Companion.of()))
    }

    /**
     * Executes the given query and returns a single matching result, or `null` if none is found.
     * Uses a Kotlin [KClass] reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param query the query to execute
     * @return the single matching result, or `null` if none found
     * @throws IllegalStateException if more than one result is found
     */
    fun <TResult : Any> findSingleOrNull(
        resultType: KClass<TResult>,
        query: FlowQuery<TRoot, TResult>
    ): TResult? {
        return findSingleOrNull(resultType.java, query)
    }

    /**
     * Executes a query built via a builder function and returns a single matching result, or `null` if none is found.
     * Uses a Kotlin [KClass] reference.
     *
     * @param TResult the result type to project to
     * @param resultType the Kotlin class of the projected result type
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the single matching result, or `null` if none found
     * @throws IllegalStateException if more than one result is found
     */
    fun <TResult : Any> findSingleOrNull(
        resultType: KClass<TResult>,
        builder: FlowQueryBuilder<TRoot, TResult>,
    ): TResult? {
        return findSingleOrNull(resultType.java, builder)
    }
}
