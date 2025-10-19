package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.*
import kotlin.reflect.KProperty1

class ExpressionWalker {
    fun <TRoot, TCurrent> walk(
        expression: Expression<TRoot, TCurrent>,
        callback: (exp: Expression<*, *>) -> ExpressionWalkerResult
    ): ExpressionWalkerResult {
        val result = callback.invoke(expression)
        if (!result.drillDown || result.replaceWith != null) {
            return result
        }

        return when (expression) {
            is AndOperator<*> -> {
                val replacements = expression.predicates.associate { it to walk(it, callback).replaceWith }
                if (replacements.values.filterNotNull().isEmpty()) {
                    ExpressionWalkerResult.Continue
                } else {
                    val replacedPredicates = expression.predicates.map {
                        (replacements[it] ?: it) as FlowPredicate<Any?>
                    }
                    ExpressionWalkerResult.Replace(
                        AndOperator(
                            replacedPredicates
                        )
                    )
                }
            }

            is IsAnyOfOperator<*, *> -> {
                val replacement = walk(expression.valueToTest, callback).replaceWith
                if (replacement != null) {
                    ExpressionWalkerResult.Replace(
                        IsAnyOfOperator(
                            replacement as Expression<Any?, Any?>,
                            expression.anyOf.map {
                                (walk(it, callback).replaceWith ?: it) as Expression<*, Any?>
                            }.toSet()
                        )
                    )
                } else {
                    ExpressionWalkerResult.Continue
                }
            }

            is IsEqual<*, *, *> -> {
                val leftReplacement = walk(expression.leftSide, callback).replaceWith
                val rightReplacement = walk(expression.rightSide, callback).replaceWith
                when {
                    leftReplacement != null && rightReplacement != null -> IsEqual(
                        leftReplacement as Expression<Any?, Any?>,
                        rightReplacement as Expression<Any?, Any?>
                    ).let {
                        ExpressionWalkerResult.Replace(it)
                    }

                    leftReplacement != null && rightReplacement == null -> IsEqual(
                        leftReplacement as Expression<Any?, Any?>,
                        expression.rightSide as Expression<Any?, Any?>
                    ).let {
                        ExpressionWalkerResult.Replace(it)
                    }

                    leftReplacement == null && rightReplacement != null -> IsEqual(
                        expression.leftSide as Expression<Any?, Any?>,
                        rightReplacement as Expression<Any?, Any?>
                    ).let {
                        ExpressionWalkerResult.Replace(it)
                    }

                    else -> ExpressionWalkerResult.Continue
                }
            }
            is NotOperator<*> -> walk(expression.expression, callback).replaceWith
                ?.let {
                    ExpressionWalkerResult.Replace(
                        NotOperator(
                            it as FlowPredicate<Any?>
                        )
                    )
                } ?: ExpressionWalkerResult.Continue

            is OrOperator<*> ->  {
                val replacements = expression.predicates.associate { it to walk(it, callback).replaceWith }
                if (replacements.values.filterNotNull().isEmpty()) {
                    ExpressionWalkerResult.Continue
                } else {
                    val replacedPredicates = expression.predicates.map {
                        (replacements[it] ?: it) as FlowPredicate<Any?>
                    }
                    ExpressionWalkerResult.Replace(
                        OrOperator(
                            replacedPredicates
                        )
                    )
                }
            }
            is PropertyExpression<*, *, *> -> walk(expression.instance, callback).replaceWith
                ?.let {
                    ExpressionWalkerResult.Replace(
                        PropertyExpression(
                            it as Expression<Any?, Any>,
                            expression.property as KProperty1<Any, Any?>
                        )
                    )
                }
                ?: ExpressionWalkerResult.Continue
            is ConjunctionExpression<*, *>, is Constant<*, *>, is Root<*> -> result
        }
    }
}