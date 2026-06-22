package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Expression
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class ExpressionNode(
    val expression: Expression<*, *>,
    val expectedType: KType? = null,
) {

    inline fun <reified T> withExpectedType(): ExpressionNode {
        return copy(
             expectedType = typeOf<T>()
        )
    }

    companion object {
        fun root(
            expression: Expression<*, *>
        ): ExpressionNode {
            return ExpressionNode(
                expression,
                null,
            )
        }
    }
}