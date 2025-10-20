package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.ConjunctionExpression
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.FlowPredicate
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.query.sorting.Sorting
import de.lise.fluxflow.query.pagination.PaginationRequest

data class QueryImpl<TRoot, TResult>(
    override val cursor: Expression<TRoot, TResult>,
    override val operations: List<QueryOperation>,
    override val pagination: PaginationRequest?,
) : Query<TRoot, TResult> {

    private fun simplifyFilter(
        op: FlowPredicate<TRoot>
    ): FlowPredicate<TRoot> {
        return when (op) {
            is PropertyExpression<TRoot, *, Boolean> -> op.isEqual(true)
            else -> op
        }
    }

    override fun where(predicate: FlowPredicate<TRoot>): Query<TRoot, TResult> {
        return copy(
            operations = operations + FilterOperation(simplifyFilter(predicate))
        )
    }

    override fun where(builder: Expression<TRoot, TResult>.() -> FlowPredicate<TRoot>): Query<TRoot, TResult> {
        return where(
            builder(cursor)
        )
    }

    override fun <TNewResult> project(projection: Expression<TResult, TNewResult>): Query<TRoot, TNewResult> {
        return QueryImpl(
            cursor = ConjunctionExpression(),
            operations = operations + ProjectionOperation(projection),
            pagination = pagination
        )
    }

    override fun <TNewResult> project(builder: Expression<TRoot, TResult>.() -> Expression<TResult, TNewResult>): Query<TRoot, TNewResult> {
        return project(
            builder(cursor)
        )
    }

    override fun sort(sorting: Sorting): Query<TRoot, TResult> {
        return copy(
            operations = operations + SortingOperation(sorting)
        )
    }

    override fun sort(builder: Expression<TRoot, TResult>.() -> Sorting): Query<TRoot, TResult> {
        return sort(
            builder(cursor)
        )
    }

    override fun limit(amount: Long): Query<TRoot, TResult> {
        return copy(
            operations = operations + LimitOperation(amount)
        )
    }

    override fun paged(pagination: PaginationRequest): Query<TRoot, TResult> {
        return copy(
            pagination = pagination
        )
    }

    override fun toString(): String {
        return toText()
    }
}