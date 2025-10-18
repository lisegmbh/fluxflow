package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.FlowPredicate
import de.fluxflow.flowquery.query.sorting.Sorting

interface Query<TRoot, TResult> {
    val operations: List<QueryOperation>

    fun where(predicate: FlowPredicate<TRoot>): Query<TRoot, TResult>
    fun where(
        builder: Expression<TRoot, TResult>.() -> FlowPredicate<TRoot>
    ): Query<TRoot, TResult>

    fun <TNewResult> project(projection: Expression<TResult, TNewResult>): Query<TRoot, TNewResult>
    fun <TNewResult> project(
        builder: Expression<TRoot, TResult>.() -> Expression<TResult, TNewResult>
    ): Query<TRoot, TNewResult>

    fun sort(sorting: Sorting): Query<TRoot, TResult>
    fun sort(
        builder: Expression<TRoot, TResult>.() -> Sorting
    ): Query<TRoot, TResult>
    
    fun toText(): String {
        return operations.joinToString("\n   | ") {
            it.toText()
        }
    }

    companion object {
        fun <TRoot> of(): Query<TRoot, TRoot> {
            return QueryImpl(
                Expression.root(),
                operations = emptyList()
            )
        }
    }
}

