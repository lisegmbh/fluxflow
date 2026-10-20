package de.fluxflow.flowquery.service

import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.FlowQueryBuilder
import de.lise.fluxflow.query.pagination.Page

/**
 * Generic service interface for querying resources based on [de.fluxflow.flowquery.query.FlowQuery] objects.
 *
 * This abstraction allows higher-level API layers to expose read operations
 * using declarative, type-safe query definitions independent of the persistence mechanism.
 *
 * @param TResource The API-layer resource type returned to clients.
 * @param TQueryable The underlying persistence or queryable entity type that
 *        can be filtered, sorted, and projected using [FlowQuery].
 *
 * ### Typical Usage
 * Implementations bridge between domain/persistence and API models — for example:
 * - Mapping a database document to a REST DTO.
 * - Translating query filters and sorts into backend queries.
 *
 * Example:
 * ```kotlin
 * class WorkflowQueryService(
 *     private val repository: FlowQueryRepository<WorkflowDocument>,
 *     private val mapper: QueryMapper<WorkflowData, WorkflowDocument>
 * ) : ResourceQueryService<WorkflowResource, WorkflowDocument> {
 *
 *     override fun getAll(): List<WorkflowResource> =
 *         repository.find(FlowQuery.of())
 *             .items
 *             .map(mapper::toResource)
 * }
 * ```
 */
interface ResourceQueryService<TResource, TQueryable> {
    /**
     * Executes the given [de.fluxflow.flowquery.query.FlowQuery] to retrieve a paginated list of [TResource]s.
     *
     * The query can contain filters, sorting, projections, and pagination information.
     *
     * @param query The [de.fluxflow.flowquery.query.FlowQuery] defining how to filter, sort, and page results.
     * @return A [de.lise.fluxflow.query.pagination.Page] containing the matching [TResource]s and pagination metadata.
     */
    fun findAll(
        query: FlowQuery<TQueryable, TQueryable>
    ): Page<TResource>

    /**
     * Builds and executes a [FlowQuery] using a [de.fluxflow.flowquery.query.FlowQueryBuilder].
     *
     * This convenience overload lets you use a lambda-style query builder:
     * ```kotlin
     * service.findAll {
     *     where { get(User::name).startsWith("A") }
     *         .sort { get(User::age).asc() }
     * }
     * ```
     *
     * @param queryBuilder A lambda that builds a [FlowQuery] from an empty base query.
     * @return A [Page] of [TResource]s matching the built query.
     */
    fun findAll(
        queryBuilder: FlowQueryBuilder<TQueryable, TQueryable>
    ): Page<TResource> {
        return findAll(
            queryBuilder(
                FlowQuery.Companion.of()
            )
        )
    }
}