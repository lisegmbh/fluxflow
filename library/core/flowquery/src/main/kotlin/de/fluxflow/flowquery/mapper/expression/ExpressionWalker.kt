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

            is StartsWithExpression<*> -> {
                val valueReplacement = walk(expression.value, callback).replaceWith
                val prefixReplacement = walk(expression.prefix, callback).replaceWith

                when {
                    valueReplacement != null || prefixReplacement != null -> StartsWithExpression(
                        value = (valueReplacement ?: expression.value) as Expression<Any?, String>,
                        prefix = (prefixReplacement ?: expression.prefix) as Expression<Any?, String>,
                        ignoreCasing = expression.ignoreCasing
                    ).let { ExpressionWalkerResult.Replace(it) }

                    else -> ExpressionWalkerResult.Continue
                }
            }

            is EndsWithExpression<*> -> {
                val valueReplacement = walk(expression.value, callback).replaceWith
                val suffixReplacement = walk(expression.suffix, callback).replaceWith

                when {
                    valueReplacement != null || suffixReplacement != null -> EndsWithExpression(
                        value = (valueReplacement ?: expression.value) as Expression<Any?, String>,
                        suffix = (suffixReplacement ?: expression.suffix) as Expression<Any?, String>,
                        ignoreCasing = expression.ignoreCasing
                    ).let { ExpressionWalkerResult.Replace(it) }

                    else -> ExpressionWalkerResult.Continue
                }
            }

            is ContainsExpression<*> -> {
                val valueReplacement = walk(expression.value, callback).replaceWith
                val substringReplacement = walk(expression.substring, callback).replaceWith

                when {
                    valueReplacement != null || substringReplacement != null -> ContainsExpression(
                        value = (valueReplacement ?: expression.value) as Expression<Any?, String>,
                        substring = (substringReplacement ?: expression.substring) as Expression<Any?, String>,
                        ignoreCasing = expression.ignoreCasing
                    ).let { ExpressionWalkerResult.Replace(it) }

                    else -> ExpressionWalkerResult.Continue
                }
            }

            is ContainsElementThatExpression<*, *, *> -> {
                val collectionReplacement = walk(expression.collection, callback).replaceWith
                val predicateReplacement = walk(expression.elementPredicate, callback).replaceWith
                when {
                    collectionReplacement != null || predicateReplacement != null -> ContainsElementThatExpression(
                        collection = (collectionReplacement ?: expression.collection) as Expression<Any?, Collection<Any?>>,
                        elementPredicate = (predicateReplacement ?: expression.elementPredicate) as Expression<Any?, Boolean>,
                    ).let { ExpressionWalkerResult.Replace(it) }

                    else -> ExpressionWalkerResult.Continue
                }
            }

            is ContainsElementExpression<*, *, *> -> {
                val collectionReplacement = walk(expression.collection, callback).replaceWith
                val elementReplacement = walk(expression.element, callback).replaceWith
                when {
                    collectionReplacement != null || elementReplacement != null -> ContainsElementExpression(
                        collection = (collectionReplacement ?: expression.collection) as Expression<Any?, Collection<*>>,
                        element = (elementReplacement ?: expression.element) as Expression<Any?, Any?>,
                    ).let { ExpressionWalkerResult.Replace(it) }

                    else -> ExpressionWalkerResult.Continue
                }
            }

            is BinaryOperationExpression<*, *, *, *> -> {
                val leftReplacement = walk(expression.leftOperand, callback).replaceWith
                val rightReplacement = walk(expression.rightOperand, callback).replaceWith
                when {
                    leftReplacement != null && rightReplacement != null -> BinaryOperationExpression<Any?, Any?, Any?, Any?>(
                        leftReplacement as Expression<Any?, Any?>,
                        expression.operation,
                        rightReplacement as Expression<Any?, Any?>
                    ).let {
                        ExpressionWalkerResult.Replace(it)
                    }

                    leftReplacement != null && rightReplacement == null -> BinaryOperationExpression<Any?, Any?, Any?, Any?>(
                        leftReplacement as Expression<Any?, Any?>,
                        expression.operation,
                        expression.rightOperand as Expression<Any?, Any?>
                    ).let {
                        ExpressionWalkerResult.Replace(it)
                    }

                    leftReplacement == null && rightReplacement != null -> BinaryOperationExpression<Any?, Any?, Any?, Any?>(
                        expression.leftOperand as Expression<Any?, Any?>,
                        expression.operation,
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

            is OrOperator<*> -> {
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