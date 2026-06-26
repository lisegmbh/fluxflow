package de.fluxflow.flowquery.expression

import kotlin.reflect.KType
import kotlin.reflect.typeOf

class NotExpression<TRoot>(
    expression: PredicateExpression<TRoot>
): PredicateExpression<TRoot> {
    override val resultType: KType = typeOf<Boolean>()

    val expression: PredicateExpression<TRoot> = simplify(expression)

    private companion object {
        fun <TRoot> simplify(expression: PredicateExpression<TRoot>): PredicateExpression<TRoot> {
            return when(expression) {
                is NotExpression -> expression.expression
                else -> expression
            }
        }
    }

    override fun toText(): String {
        return "NOT(${expression.toText()})"
    }

    override fun toString(): String {
        return toText()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is NotExpression<*>) {
            return false
        }
        return expression == other.expression
    }

    override fun hashCode(): Int {
        return expression.hashCode()
    }
}