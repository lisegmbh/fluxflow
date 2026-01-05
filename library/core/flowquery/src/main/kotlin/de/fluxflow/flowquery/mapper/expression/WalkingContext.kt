package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

data class WalkingContext(
    val parent: WalkingContext?,
    val expression: Expression<*, *>,
    val level: Int,
) {

    fun sub(expression: Expression<*, *>): WalkingContext {
        return WalkingContext(
            parent = this,
            expression = expression,
            level = level + 1
        )
    }
}