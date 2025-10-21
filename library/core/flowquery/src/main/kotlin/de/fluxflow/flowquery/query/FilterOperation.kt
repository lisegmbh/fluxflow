package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.PredicateExpression

data class FilterOperation(
    val predicate: PredicateExpression<*>
): QueryOperation {
    override fun toText(): String {
        return "WHERE ${predicate.toText()}"
    }

    override fun toString(): String {
        return toText()
    }
}

