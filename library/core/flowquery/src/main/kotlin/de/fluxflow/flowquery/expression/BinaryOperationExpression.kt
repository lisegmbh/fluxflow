package de.fluxflow.flowquery.expression

data class BinaryOperationExpression<TRoot, TLeftOperand, TRightOperand>(
    val leftOperand: Expression<TRoot, TLeftOperand>,
    val operation: BinaryOperation,
    val rightOperand: Expression<TRoot, TRightOperand>
): LogicalExpression<TRoot> {
    override fun toText(): String {
        return "${leftOperand.toText()} ${operation.symbol} ${rightOperand.toText()}"
    }

    override fun toString(): String {
        return toText()
    }
}