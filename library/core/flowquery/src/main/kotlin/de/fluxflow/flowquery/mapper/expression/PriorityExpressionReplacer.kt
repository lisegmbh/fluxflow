package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression

open class PriorityExpressionReplacer(
    private val expressionReplacer: List<ExpressionReplacer>,
) : ExpressionReplacer {
    override fun replace(
        node: ExpressionNode
    ): Expression<*, *>? {
        return ExpressionWalker().walk(node) { context ->
            doProcess(context.current)
        }.replaceWith
    }

    private fun doProcess(node: ExpressionNode): ExpressionWalkerResult {
        val firstResult = expressionReplacer.firstNotNullOfOrNull { replacer ->
            replacer.replace(node)
        } ?: return ExpressionWalkerResult.Continue

        // Recurse
        val recursionResult = replace(
            ExpressionNode.root(
                firstResult
            )
        )
        return when(recursionResult) {
            null -> ExpressionWalkerResult.Replace(firstResult)
            else -> ExpressionWalkerResult.Replace(recursionResult)
        }
    }
}