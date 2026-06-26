package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf


data class BinaryOperationExpression<TRoot, TLeftOperand, TRightOperand>(
    val leftOperand: Expression<TRoot, TLeftOperand>,
    val operation: BinaryOperation,
    val rightOperand: Expression<TRoot, TRightOperand>
): PredicateExpression<TRoot> {
    override val resultType: KType = typeOf<Boolean>()

    override fun toText(): String {
        return "${leftOperand.toText()} ${operation.symbol} ${rightOperand.toText()}"
    }

    override fun toString(): String {
        return toText()
    }
}