package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

class PriorityExpressionReplacer(
    private val expressionReplacer: List<ExpressionReplacer>
) : ExpressionReplacer {
    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return ExpressionWalker().walk(expression) { currentExp ->
            doProcess(currentExp)
        }.replaceWith
    }

    private fun doProcess(currentExp: Expression<*, *>): ExpressionWalkerResult {
        val firstResult = expressionReplacer.firstNotNullOfOrNull { replacer ->
            replacer.replace(currentExp)
        } ?: return ExpressionWalkerResult.Continue

        // Recurse
        val recursionResult = replace(firstResult)
        return when(recursionResult) {
            null -> ExpressionWalkerResult.Replace(firstResult)
            else -> ExpressionWalkerResult.Replace(recursionResult)
        }
    }
}

