package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

data class WalkingContext(
    val parent: WalkingContext?,
    val current: ExpressionNode,
    val level: Int,
) {

    fun sub(node: ExpressionNode): WalkingContext {
        return WalkingContext(
            parent = this,
            current = node,
            level = level + 1
        )
    }

    fun sub(expression: Expression<*,*>): WalkingContext {
        return sub(
            ExpressionNode.root(expression)
        )
    }
}