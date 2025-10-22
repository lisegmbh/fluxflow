package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.*
import de.fluxflow.flowquery.query.sorting.Sorting
import de.lise.fluxflow.query.pagination.PaginationRequest

data class FlowQueryImpl<TRoot, TResult>(
    override val cursor: Expression<TRoot, TResult>,
    override val operations: List<QueryOperation>,
    override val pagination: PaginationRequest?,
) : FlowQuery<TRoot, TResult> {

    private fun simplifyFilter(
        op: PredicateExpression<TRoot>
    ): PredicateExpression<TRoot> {
        return when (op) {
            is PropertyExpression<TRoot, *, Boolean> -> op.isEqual(true)
            else -> op
        }
    }

    override fun where(predicate: PredicateExpression<TRoot>): FlowQuery<TRoot, TResult> {
        return copy(
            operations = operations + FilterOperation(simplifyFilter(predicate))
        )
    }

    override fun where(builder:  ExpressionBuilder<TRoot, TResult, Boolean>): FlowQuery<TRoot, TResult> {
        return where(
            builder(cursor)
        )
    }

    override fun <TNewResult> project(projection: Expression<TResult, TNewResult>): FlowQuery<TRoot, TNewResult> {
        return FlowQueryImpl(
            cursor = ConjunctionExpression(),
            operations = operations + ProjectionOperation(projection),
            pagination = pagination
        )
    }

    override fun <TNewResult> project(builder: Expression<TRoot, TResult>.() -> Expression<TResult, TNewResult>): FlowQuery<TRoot, TNewResult> {
        return project(
            builder(cursor)
        )
    }

    override fun sort(sorting: Sorting): FlowQuery<TRoot, TResult> {
        return copy(
            operations = operations + SortingOperation(sorting)
        )
    }

    override fun sort(builder: Expression<TRoot, TResult>.() -> Sorting): FlowQuery<TRoot, TResult> {
        return sort(
            builder(cursor)
        )
    }

    override fun limit(amount: Long): FlowQuery<TRoot, TResult> {
        return copy(
            operations = operations + LimitOperation(amount)
        )
    }

    override fun paged(pagination: PaginationRequest): FlowQuery<TRoot, TResult> {
        return copy(
            pagination = pagination
        )
    }

    override fun append(other: FlowQuery<TRoot, TResult>): FlowQuery<TRoot, TResult> {
        return FlowQueryImpl(
            cursor = other.cursor,
            operations = operations + other.operations,
            pagination = other.pagination ?: this.pagination
        )
    }

    override fun toString(): String {
        return toText()
    }
}