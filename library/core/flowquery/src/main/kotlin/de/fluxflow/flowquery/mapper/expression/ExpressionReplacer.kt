package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.ConstantExpression
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import kotlin.reflect.KProperty1

fun interface ExpressionReplacer {
    fun replace(node: ExpressionNode): Expression<*, *>?

    fun recursive(): ExpressionReplacer {
        return PriorityExpressionReplacer(
            listOf(
                this
            )
        )
    }
    
    fun toMapper(): ExpressionMapper {
        return ExpressionMapper {
            replace(it) ?: it.expression
        }
    }
    
    companion object {
        inline fun <reified T> constantOfType(
            crossinline replacement: (exp: T) -> Expression<*,*>
        ): ExpressionReplacer {
            return ExpressionReplacer {
                when(val exp = it.expression) {
                    is ConstantExpression<*,*> if exp.value is T -> replacement(exp.value)
                    else -> null
                }
            }
        }
        
        fun property(
            prop: KProperty1<*,*>,
            replacement: (exp: PropertyExpression<*,*,*>) -> Expression<*,*>?
        ): ExpressionReplacer {
            return ExpressionReplacer {
                when(val exp = it.expression) {
                    is PropertyExpression<*,*,*> -> when(exp.property) {
                        prop -> replacement(exp)
                        else -> null
                    }
                    else -> null
                }
            }
        }

        fun <TSource, TTarget, TProperty> property(
            sourceProperty: KProperty1<TSource, TProperty>,
            targetProperty: KProperty1<TTarget, TProperty>
        ): ExpressionReplacer {
            return property(sourceProperty) {
                PropertyExpression(
                    it.instance as Expression<Any, TTarget>,
                    targetProperty
                )
            }
        }
        
        inline fun <reified TDomainValue, TProperty> domainValue(
            valueProperty: KProperty1<TDomainValue, TProperty>
        ): ExpressionReplacer {
            return PriorityExpressionReplacer(
                listOf(
                    constantOfType<TDomainValue> {
                        ConstantExpression<Any, TProperty>(
                            valueProperty.get(it)
                        )
                    },
                    property(valueProperty) {
                        it.instance
                    }
                )
            )
        }
    }
}
