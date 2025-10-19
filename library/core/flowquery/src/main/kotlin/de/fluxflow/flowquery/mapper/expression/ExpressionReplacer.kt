package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.Constant
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import kotlin.reflect.KProperty1

fun interface ExpressionReplacer {
    fun replace(expression: Expression<*, *>): Expression<*, *>?

    companion object {
        inline fun <reified T> constantOfType(
            crossinline replacement: (exp: T) -> Expression<*,*>
        ): ExpressionReplacer {
            return ExpressionReplacer {
                when(it) {
                    is Constant<*,*> if it.value is T -> replacement(it.value as T)
                    else -> null
                }
            }
        }

        fun property(
            prop: KProperty1<*,*>,
            replacement: (exp: PropertyExpression<*,*,*>) -> Expression<*,*>?
        ): ExpressionReplacer {
            return ExpressionReplacer {
                when(it) {
                    is PropertyExpression<*,*,*> -> when(it.property) {
                        prop -> replacement(it)
                        else -> null
                    }
                    else -> null
                }
            }
        }
    }
}
