package de.fluxflow.flowquery.repository

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.FlowQueryBuilder
import de.lise.fluxflow.query.pagination.Page

/**
 * Defines a repository interface for executing [FlowQuery] objects that operate directly on
 * the root entity type [TRoot], without projecting to a different result type.
 *
 * This interface complements [ProjectingRepository], which supports type-safe projections
 * to arbitrary result types. In contrast, a [NonProjectingRepository] is typically used for
 * queries that return complete root entities, often representing database records or domain models.
 *
 * @param TRoot the root entity or domain type managed by this repository
 */
interface NonProjectingRepository<TRoot> {

    // --------------------------------------------------------------------------------------------
    // find (paged results)
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns a paged list of matching root entities.
     *
     * Example:
     * ```
     * val page = repository.find(
     *     FlowQuery.of<User>().where { get(User::isActive).isEqual(true) }
     * )
     * ```
     *
     * @param query the query to execute
     * @return a [Page] containing the matching root entities
     */
    fun find(query: FlowQuery<TRoot, TRoot>): Page<TRoot>

    /**
     * Executes a query built via a builder function and returns a paged list of matching root entities.
     *
     * Example:
     * ```
     * val page = repository.find {
     *     it.where { get(User::isActive).isEqual(true) }.paged(pageIndex = 0, pageSize = 20)
     * }
     * ```
     *
     * @param builder a function that builds the [FlowQuery] to execute
     * @return a [Page] containing the matching root entities
     */
    fun find(
        builder: FlowQueryBuilder<TRoot, TRoot>
    ): Page<TRoot> {
        return find(builder(FlowQuery.Companion.of()))
    }


    // --------------------------------------------------------------------------------------------
    // findFirst
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns the first matching root entity.
     *
     * Example:
     * ```
     * val user = repository.findFirst(
     *     FlowQuery.of<User>().where { get(User::isActive).isEqual(true) }
     * )
     * ```
     *
     * @param query the query to execute
     * @return the first matching root entity
     * @throws NoSuchElementException if no entity matches the query
     */
    fun findFirst(query: FlowQuery<TRoot, TRoot>): TRoot

    /**
     * Executes a query built via a builder function and returns the first matching root entity.
     *
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the first matching root entity
     * @throws NoSuchElementException if no entity matches the query
     */
    fun findFirst(
        builder: FlowQueryBuilder<TRoot, TRoot>,
    ): TRoot {
        return findFirst(builder(FlowQuery.Companion.of()))
    }


    // --------------------------------------------------------------------------------------------
    // findFirstOrNull
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns the first matching root entity,
     * or `null` if none is found.
     *
     * Example:
     * ```
     * val user = repository.findFirstOrNull(
     *     FlowQuery.of<User>().where { get(User::id).isEqual(42) }
     * )
     * ```
     *
     * @param query the query to execute
     * @return the first matching root entity, or `null` if none found
     */
    fun findFirstOrNull(query: FlowQuery<TRoot, TRoot>): TRoot?

    /**
     * Executes a query built via a builder function and returns the first matching root entity,
     * or `null` if none is found.
     *
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the first matching root entity, or `null` if none found
     */
    fun findFirstOrNull(
        builder: FlowQueryBuilder<TRoot, TRoot>
    ): TRoot? {
        return findFirstOrNull(builder(FlowQuery.Companion.of()))
    }


    // --------------------------------------------------------------------------------------------
    // findSingle
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns exactly one matching root entity.
     *
     * Example:
     * ```
     * val user = repository.findSingle(
     *     FlowQuery.of<User>().where { get(User::email).isEqual("a@b.com") }
     * )
     * ```
     *
     * @param query the query to execute
     * @return the single matching root entity
     * @throws NoSuchElementException if no entity matches the query
     * @throws IllegalStateException if more than one entity matches the query
     */
    fun findSingle(query: FlowQuery<TRoot, TRoot>): TRoot

    /**
     * Executes a query built via a builder function and returns exactly one matching root entity.
     *
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the single matching root entity
     * @throws NoSuchElementException if no entity matches the query
     * @throws IllegalStateException if more than one entity matches the query
     */
    fun findSingle(
        builder: FlowQueryBuilder<TRoot, TRoot>
    ): TRoot {
        return findSingle(builder(FlowQuery.Companion.of()))
    }


    // --------------------------------------------------------------------------------------------
    // findSingleOrNull
    // --------------------------------------------------------------------------------------------

    /**
     * Executes the given query and returns a single matching root entity,
     * or `null` if none is found.
     *
     * Example:
     * ```
     * val user = repository.findSingleOrNull(
     *     FlowQuery.of<User>().where { get(User::username).isEqual("marcel") }
     * )
     * ```
     *
     * @param query the query to execute
     * @return the single matching root entity, or `null` if none found
     * @throws IllegalStateException if more than one entity matches the query
     */
    fun findSingleOrNull(query: FlowQuery<TRoot, TRoot>): TRoot?

    /**
     * Executes a query built via a builder function and returns a single matching root entity,
     * or `null` if none is found.
     *
     * @param builder a function that builds the [FlowQuery] to execute
     * @return the single matching root entity, or `null` if none found
     * @throws IllegalStateException if more than one entity matches the query
     */
    fun findSingleOrNull(
        builder: FlowQueryBuilder<TRoot, TRoot>
    ): TRoot? {
        return findSingleOrNull(builder(FlowQuery.Companion.of()))
    }
}
