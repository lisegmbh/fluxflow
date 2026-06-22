package de.lise.fluxflow.springboot.rest.workflow.step

import de.fluxflow.flowquery.expression.BinaryOperationExpression
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.IsAnyOfOperator
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

class EnumReplacer : ExpressionReplacer {
    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return when (expression) {
            is BinaryOperationExpression<*, *, *> -> {
                val left = expression.leftOperand.returnType
                if(left?.isSubtypeOf(typeOf<Enum<*>>()) == true) {
                    val replacement = ConstantReplacer.forEnum<Any, Enum<*>>(left.classifier as KClass<Enum<*>>)
                        .recursive()
                        .replace(expression)
                    replacement
                } else {
                    null
                }
            }
            is IsAnyOfOperator<*, *> -> {
                TODO()
            }
            else -> null
        }
    }
}