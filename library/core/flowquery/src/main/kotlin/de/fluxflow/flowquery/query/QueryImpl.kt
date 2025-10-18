package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.ConjunctionExpression
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.FlowPredicate
import de.fluxflow.flowquery.query.sorting.Sorting

data class QueryImpl<TRoot, TResult>(
    private val cursor: Expression<TRoot, TResult>,
    override val operations: List<QueryOperation>
) : Query<TRoot, TResult> {
    override fun where(predicate: FlowPredicate<TRoot>): Query<TRoot, TResult> {
        return copy(
            operations = operations + FilterOperation(predicate)
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

    override fun toString(): String {
        return toText()
    }
}