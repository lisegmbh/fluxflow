package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

data class ExpressionWalkerResult(
    val replaceWith: Expression<*, *>?,
    val drillDown: Boolean
) {

    companion object {
        val Continue = ExpressionWalkerResult(
            replaceWith = null,
            drillDown = true
        )

        fun Replace(
            expression: Expression<*, *>
        ): ExpressionWalkerResult {
            return ExpressionWalkerResult(
                expression,
                true
            )
        }
    }

}