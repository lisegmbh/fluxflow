package de.fluxflow.flowquery.expression

data class BinaryOperationExpression<TRoot, TLeftOperand, TRightOperand, TResult>(
    val leftOperand: Expression<TRoot, TLeftOperand>,
    val operation: BinaryOperation,
    val rightOperand: Expression<TRoot, TRightOperand>
): Expression<TRoot, TResult> {
    override fun toText(): String {
        return "${leftOperand.toText()} ${operation.symbol} ${rightOperand.toText()}"
    }

    override fun toString(): String {
        return toText()
    }
}