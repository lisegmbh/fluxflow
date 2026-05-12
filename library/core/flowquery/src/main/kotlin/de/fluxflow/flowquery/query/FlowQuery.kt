package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.ExpressionBuilder
import de.fluxflow.flowquery.expression.PredicateExpression
import de.fluxflow.flowquery.query.sorting.Sorting
import de.lise.fluxflow.query.pagination.PaginationRequest

/**
 * Represents a composable, type-safe query structure that operates over a given root type [TRoot]
 * and produces results of type [TResult].
 *
 * A [FlowQuery] models a chain of declarative query operations such as filtering, projection,
 * sorting, and pagination. It forms an immutable representation of a query that can later be
 * translated to an executable form (e.g. SQL, in-memory filtering, or API requests).
 *
 * @param TRoot the root entity or data type of the query
 * @param TResult the current result type produced by the query
 */
interface FlowQuery<TRoot, TResult> {

    /**
     * The root expression or "cursor" of the query, representing the current scope of [TResult].
     */
    val cursor: Expression<TRoot, TResult>

    /**
     * The list of operations applied to this query, such as filters, projections, or sorts.
     */
    val operations: List<QueryOperation>

    /**
     * The pagination configuration of this query, if any.
     */
    val pagination: PaginationRequest?

    // --------------------------------------------------------------------------------------------
    // Filtering
    // --------------------------------------------------------------------------------------------

    /**
     * Adds a filter predicate to the query.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .where(user.get(User::isActive).isEqual(true))
     * ```
     *
     * @param predicate the predicate expression to filter by
     * @return a new [FlowQuery] instance with the added filter condition
     */
    fun where(predicate: PredicateExpression<TRoot>): FlowQuery<TRoot, TResult>

    /**
     * Adds a filter predicate to the query using a builder function.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .where { get(User::age).isGreaterThan(18) }
     * ```
     *
     * @param builder a builder function creating a boolean predicate expression from the current scope
     * @return a new [FlowQuery] instance with the added filter condition
     */
    fun where(
        builder: ExpressionBuilder<TRoot, TResult, Boolean>
    ): FlowQuery<TRoot, TResult>


    // --------------------------------------------------------------------------------------------
    // Projection
    // --------------------------------------------------------------------------------------------

    /**
     * Projects the current result type to another type using the given expression.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .project(user.get(User::name))
     * ```
     *
     * @param projection the expression defining the projection
     * @return a new [FlowQuery] producing results of the projected type
     */
    fun <TNewResult> project(projection: Expression<TResult, TNewResult>): FlowQuery<TRoot, TNewResult>

    /**
     * Projects the current result type to another type using a builder function.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .project { get(User::email) }
     * ```
     *
     * @param builder a builder function that produces the projection expression
     * @return a new [FlowQuery] producing results of the projected type
     */
    fun <TNewResult> project(
        builder: Expression<TRoot, TResult>.() -> Expression<TResult, TNewResult>
    ): FlowQuery<TRoot, TNewResult>


    // --------------------------------------------------------------------------------------------
    // Sorting
    // --------------------------------------------------------------------------------------------

    /**
     * Adds a sorting operation to the query.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .sort(Sorting.asc(user.get(User::name)))
     * ```
     *
     * @param sorting the sorting configuration to apply
     * @return a new [FlowQuery] instance with the added sorting operation
     */
    fun sort(sorting: Sorting): FlowQuery<TRoot, TResult>

    /**
     * Adds a sorting operation to the query using a builder function.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .sort { Sorting.desc(get(User::createdAt)) }
     * ```
     *
     * @param builder a builder function creating a [Sorting] object
     * @return a new [FlowQuery] instance with the added sorting operation
     */
    fun sort(
        builder: Expression<TRoot, TResult>.() -> Sorting
    ): FlowQuery<TRoot, TResult>


    // --------------------------------------------------------------------------------------------
    // Limiting and Pagination
    // --------------------------------------------------------------------------------------------

    /**
     * Limits the number of results returned by the query.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .limit(100)
     * ```
     *
     * @param amount the maximum number of results to return
     * @return a new [FlowQuery] instance with the result limit applied
     */
    fun limit(amount: Long): FlowQuery<TRoot, TResult>

    /**
     * Applies a pagination request to the query.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .paged(PaginationRequest(pageIndex = 0, pageSize = 20))
     * ```
     *
     * @param pagination the pagination configuration to apply
     * @return a new [FlowQuery] instance with pagination applied
     */
    fun paged(pagination: PaginationRequest): FlowQuery<TRoot, TResult>

    /**
     * Applies pagination to the query using explicit page index and size values.
     *
     * Example:
     * ```
     * val query = FlowQuery.of<User>()
     *     .paged(pageIndex = 2, pageSize = 50)
     * ```
     *
     * @param pageIndex the zero-based index of the page to retrieve
     * @param pageSize the number of elements per page
     * @return a new [FlowQuery] instance with pagination applied
     */
    fun paged(
        pageIndex: Int,
        pageSize: Int
    ): FlowQuery<TRoot, TResult> {
        return paged(
            PaginationRequest(
                pageIndex = pageIndex,
                pageSize = pageSize
            )
        )
    }


    // --------------------------------------------------------------------------------------------
    // Utility
    // --------------------------------------------------------------------------------------------

    /**
     * Returns a textual representation of the query for debugging or logging.
     *
     * Example:
     * ```
     * println(query.toText())
     * ```
     *
     * @return a string representation of the query and its operations
     */
    fun toText(): String {
        val operationsText = operations.joinToString("\n   | ") {
            it.toText()
        }
        return when(pagination) {
            null -> "$operationsText\n   | UNPAGINATED"
            else -> "$operationsText\n   | PAGINATED (page=${pagination?.pageIndex}, size=${pagination?.pageSize})"
        }
    }

    /**
     * Adds a filter only if [value] is considered present.
     *
     * A value is considered absent if it is:
     * - `null`
     * - a blank [CharSequence]
     * - an empty [Collection]
     * - an empty [Map]
     * - an empty [Array]
     *
     * Example:
     * ```
     * val cityFilter: String? = request.city
     *
     * val query = FlowQuery.of<WorkflowQueryable>()
     *     .ifPresent(cityFilter) { city ->
     *         get(WorkflowQueryable::model)
     *             .get(PizzaOrder::city)
     *             .isEqual(city)
     *     }
     * ```
     *
     * @param value the optional value that controls whether the filter is added
     * @param builder builds the predicate using the present, non-null [value]
     * @return the unchanged query if [value] is absent; otherwise a new query with the added filter
     */
    fun <TValue> ifPresent(
        value: TValue?,
        builder: Expression<TRoot, TResult>.(TValue) -> Expression<TRoot, Boolean>,
    ): FlowQuery<TRoot, TResult> {
        val presentValue = value ?: return this
        val isAbsent = when (presentValue) {
            is CharSequence -> presentValue.isBlank()
            is Collection<*> -> presentValue.isEmpty()
            is Map<*, *> -> presentValue.isEmpty()
            is Array<*> -> presentValue.isEmpty()
            else -> false
        }

        if (isAbsent) {
            return this
        }

        return where {
            builder(presentValue)
        }
    }

    /**
     * Appends another query's operations to this one, preserving the current root and result type.
     *
     * Example:
     * ```
     * val combined = query1.append(query2)
     * ```
     *
     * @param other the query whose operations should be appended
     * @return a new [FlowQuery] with the combined operations
     */
    fun append(other: FlowQuery<TRoot, TResult>): FlowQuery<TRoot, TResult>


    // --------------------------------------------------------------------------------------------
    // Factory Methods
    // --------------------------------------------------------------------------------------------

    companion object {

        /**
         * Creates a new [FlowQuery] whose root and result type are the same.
         *
         * Example:
         * ```
         * val query = FlowQuery.of<User>()
         * ```
         *
         * @param TRoot the root entity or type of the query
         * @return a new [FlowQuery] instance initialized with an empty operation list
         */
        fun <TRoot> of(): FlowQuery<TRoot, TRoot> {
            return FlowQueryImpl(
                Expression.root(),
                operations = emptyList(),
                pagination = null
            )
        }

        /**
         * Creates a new [FlowQuery] using a builder function.
         *
         * Example:
         * ```
         * val query = FlowQuery.of<User, String> {
         *     it.where { get(User::isActive).isEqual(true) }
         *       .project { get(User::name) }
         * }
         * ```
         *
         * @param builder a builder function that configures and returns a [FlowQuery]
         * @return a new [FlowQuery] instance created by applying the builder
         */
        fun <TRoot, TResult> of(
            builder: FlowQueryBuilder<TRoot, TResult>
        ): FlowQuery<TRoot, TResult> {
            return builder(of())
        }
    }
}
