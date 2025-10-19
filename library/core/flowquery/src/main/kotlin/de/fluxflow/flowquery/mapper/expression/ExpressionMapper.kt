package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

fun interface ExpressionMapper : ExpressionReplacer {
    override fun replace(expression: Expression<*, *>): Expression<*, *>
}