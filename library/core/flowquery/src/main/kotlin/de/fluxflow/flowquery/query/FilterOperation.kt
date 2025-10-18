package de.fluxflow.flowquery.query

import de.fluxflow.flowquery.expression.FlowPredicate

class FilterOperation(
    val predicated: FlowPredicate<*>
): QueryOperation {

    override fun toText(): String {
        return "WHERE ${predicated.toText()}"
    }

    override fun toString(): String {
        return toText()
    }

}