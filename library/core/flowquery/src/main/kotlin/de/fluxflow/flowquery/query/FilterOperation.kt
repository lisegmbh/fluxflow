package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.FlowPredicate

data class FilterOperation(
    val predicate: FlowPredicate<*>
): QueryOperation {
    override fun toText(): String {
        return "WHERE ${predicate.toText()}"
    }

    override fun toString(): String {
        return toText()
    }
}

