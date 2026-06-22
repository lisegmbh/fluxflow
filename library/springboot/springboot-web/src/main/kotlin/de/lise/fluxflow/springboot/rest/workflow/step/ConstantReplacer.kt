package de.lise.fluxflow.springboot.rest.workflow.step

import de.fluxflow.flowquery.expression.ConstantExpression
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import kotlin.reflect.KClass

class ConstantReplacer<TSource : Any, TTarget>(
    private val sourceType: KClass<TSource>,
    private val replacementMapping: Map<TSource, Expression<*, TTarget>?>,
    private val defaultResult: Expression<*, TTarget>?
) : ExpressionReplacer {
    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        if(expression is ConstantExpression<*, *>) {
            val value = expression.value
            if(sourceType.isInstance(value)) {
                return if(replacementMapping.contains(value)) {
                    replacementMapping[value]
                } else {
                    defaultResult
                }
            }
        }
        return null
    }


    companion object {
        fun <TRoot : Any, T : Enum<*>> forEnum(
            type: KClass<T>,
            defaultReplacement: Expression<*, T>? = null
        ): ConstantReplacer<String, T> {
            val all =  type.java.enumConstants.associate { it: T ->
                it.name to Expression.Companion.const<TRoot, T>(it)
            }
            return ConstantReplacer(
                sourceType = String::class,
                replacementMapping = all,
                defaultResult = defaultReplacement,
            )
        }

        inline fun <TRoot : Any, reified T: Enum<T>> forEnum(
            defaultReplacement: Expression<*, T>? = null
        ): ConstantReplacer<String, T> {
            return forEnum<TRoot, T>(
                T::class,
                defaultReplacement
            )
        }
    }
}