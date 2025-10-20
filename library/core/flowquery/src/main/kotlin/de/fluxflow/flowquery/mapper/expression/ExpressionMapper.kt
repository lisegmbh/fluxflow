package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

fun interface ExpressionMapper : ExpressionReplacer {
    fun mapOrKeep(expression: Expression<*, *>): Expression<*, *> {
        return replace(expression) ?: expression
    }
}