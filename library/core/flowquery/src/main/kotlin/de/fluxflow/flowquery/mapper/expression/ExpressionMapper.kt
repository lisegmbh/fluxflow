package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

fun interface ExpressionMapper {
    fun map(node: ExpressionNode): Expression<*,*>
}