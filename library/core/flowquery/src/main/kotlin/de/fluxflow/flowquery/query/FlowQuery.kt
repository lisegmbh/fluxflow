package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.ExpressionBuilder
import de.fluxflow.flowquery.expression.PredicateExpression
import de.fluxflow.flowquery.query.sorting.Sorting
import de.lise.fluxflow.query.pagination.PaginationRequest

interface FlowQuery<TRoot, TResult> {
    val cursor: Expression<TRoot, TResult>
    val operations: List<QueryOperation>
    val pagination: PaginationRequest?

    fun where(predicate: PredicateExpression<TRoot>): FlowQuery<TRoot, TResult>
    fun where(
        builder: ExpressionBuilder<TRoot, TResult, Boolean>
    ): FlowQuery<TRoot, TResult>

    fun <TNewResult> project(projection: Expression<TResult, TNewResult>): FlowQuery<TRoot, TNewResult>
    fun <TNewResult> project(
        builder: Expression<TRoot, TResult>.() -> Expression<TResult, TNewResult>
    ): FlowQuery<TRoot, TNewResult>

    fun sort(sorting: Sorting): FlowQuery<TRoot, TResult>
    fun sort(
        builder: Expression<TRoot, TResult>.() -> Sorting
    ): FlowQuery<TRoot, TResult>

    fun limit(amount: Long): FlowQuery<TRoot, TResult>

    fun paged(pagination: PaginationRequest): FlowQuery<TRoot, TResult>
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

    fun toText(): String {
        return operations.joinToString("\n   | ") {
            it.toText()
        }
    }

    companion object {
        fun <TRoot> of(): FlowQuery<TRoot, TRoot> {
            return FlowQueryImpl(
                Expression.root(),
                operations = emptyList(),
                pagination = null
            )
        }

        fun <TRoot, TResult> of(
            builder: FlowQueryBuilder<TRoot, TResult>
        ): FlowQuery<TRoot, TResult> {
            return builder(
                of()
            )
        }
    }
}

