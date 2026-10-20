package de.fluxflow.flowquery.mapper.expression

import de.fluxflow.flowquery.expression.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class ExpressionWalker {
    fun walk(
        expression: Expression<*, *>,
        callback: ExpressionWalkerCallback
    ): ExpressionWalkerResult {
        return walk(
            WalkingContext(
                parent = null,
                expression = expression,
                level = 0
            ),
            callback
        )
    }
    
    private fun walk(
        context: WalkingContext,
        callback: ExpressionWalkerCallback,
    ): ExpressionWalkerResult {
        val result = callback.invoke(context)
        if (!result.drillDown || result.replaceWith != null) {
            return result
        }

        return when (val expression = context.expression) {
            is CastExpression<*, *, *> -> {
                val instanceReplacement = walk(
                    context.sub(expression.instance),
                    callback
                ).replaceWith
                when (instanceReplacement) {
                    null -> ExpressionWalkerResult.Continue
                    else -> ExpressionWalkerResult.Replace(
                        CastExpression(
                            instanceReplacement as Expression<Any?, Any>,
                            expression.requiredType as KClass<Any>
                        )
                    )
                }
            }

            is IsTypeExpression<*, *, *> -> {
                val instanceReplacement = walk(
                    context.sub(expression.instance),
                    callback
                ).replaceWith
                when (instanceReplacement) {
                    null -> ExpressionWalkerResult.Continue
                    else -> ExpressionWalkerResult.Replace(
                        IsTypeExpression(
                            instanceReplacement as Expression<Any?, Any?>,
                            expression.requiredType as KClass<Any>
                        )
                    )
                }
            }

            is AndExpression<*> -> {
                val replacements = expression.predicates.associateWith {
                    walk(
                        context.sub(it),
                        callback
                    ).replaceWith
                }
                if (replacements.values.filterNotNull().isEmpty()) {
                    ExpressionWalkerResult.Continue
                } else {
                    val replacedPredicates = expression.predicates.map {
                        (replacements[it] ?: it) as PredicateExpression<Any?>
                    }
                    ExpressionWalkerResult.Replace(
                        AndExpression(
                            replacedPredicates
                        )
                    )
                }
            }

            is IsAnyOfOperator<*, *> -> {
                val valueToTestReplacement = walk(
                    context.sub(expression.valueToTest),
                    callback
                ).replaceWith

                val anyOfReplacements = expression.anyOf.associateWith {
                    walk(
                        context.sub(it),
                        callback
                    ).replaceWith
                }

                if (
                    valueToTestReplacement != null
                    || anyOfReplacements.values.filterNotNull().isNotEmpty()
                ) {
                    ExpressionWalkerResult.Replace(
                        IsAnyOfOperator(
                            (valueToTestReplacement ?: expression.valueToTest) as Expression<Any?, Any?>,
                            anyOfReplacements.entries.map {
                                (it.value ?: it.key) as Expression<*, Any?>
                            }.toSet()
                        )
                    )
                } else {
                    ExpressionWalkerResult.Continue
                }
            }

            is StartsWithExpression<*> -> {
                val valueReplacement = walk(
                    context.sub(expression.value),
                    callback
                ).replaceWith
                val prefixReplacement = walk(
                    context.sub(expression.prefix),
                    callback
                ).replaceWith

                when {
                    valueReplacement != null || prefixReplacement != null -> StartsWithExpression(
                        value = (valueReplacement ?: expression.value) as Expression<Any?, String>,
                        prefix = (prefixReplacement ?: expression.prefix) as Expression<Any?, String>,
                        ignoreCasing = expression.ignoreCasing
                    ).let { ExpressionWalkerResult.Replace(it) }

                    else -> ExpressionWalkerResult.Continue
                }
            }

            is MapAccessExpression<*, *, *, *> -> {
                val instanceReplacement = walk(
                    context.sub(expression.instance),
                    callback
                ).replaceWith
                val keyReplacement = walk(
                    context.sub(expression.key),
                    callback
                ).replaceWith

                when {
                    instanceReplacement != null || keyReplacement != null -> MapAccessExpression(
                        instance = (instanceReplacement ?: expression.instance) as Expression<Any?, Map<Any?, Any?>>,
                        key = (keyReplacement ?: expression.key) as Expression<Any?, Any?>
                    ).let {
                        ExpressionWalkerResult.Replace(it)
                    }

                    else -> ExpressionWalkerResult.Continue
                }
            }
            
            is HasKeyExpression<*,*> -> {
                val instanceReplacement = walk(
                    context.sub(expression.instance),
                    callback
                ).replaceWith
                val keyReplacement = walk(
                    context.sub(expression.key),
                    callback
                ).replaceWith
                
                when {
                    instanceReplacement != null || keyReplacement != null -> HasKeyExpression(
                        instance = (instanceReplacement ?: expression.instance) as Expression<Any?, Map<Any?, Any?>>,
                        key = (keyReplacement ?: expression.key) as Expression<Any?, Any?>
                    ).let {
                        ExpressionWalkerResult.Replace(it)
                    }
                    
                    else -> ExpressionWalkerResult.Continue
                }
            }

            is EndsWithExpression<*> -> {
                val valueReplacement = walk(
                    context.sub(expression.value),
                    callback
                ).replaceWith
                val suffixReplacement = walk(
                    context.sub(expression.suffix),
                    callback
                ).replaceWith

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
                val valueReplacement = walk(
                    context.sub(expression.value),
                    callback
                ).replaceWith
                val substringReplacement = walk(
                    context.sub(expression.substring),
                    callback
                ).replaceWith

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
                val collectionReplacement = walk(
                    context.sub(expression.collection),
                    callback
                ).replaceWith
                val predicateReplacement = walk(
                    context.sub(expression.elementPredicate),
                    callback
                ).replaceWith
                when {
                    collectionReplacement != null || predicateReplacement != null -> ContainsElementThatExpression(
                        collection = (collectionReplacement
                            ?: expression.collection) as Expression<Any?, Collection<Any?>>,
                        elementPredicate = (predicateReplacement
                            ?: expression.elementPredicate) as Expression<Any?, Boolean>,
                    ).let { ExpressionWalkerResult.Replace(it) }

                    else -> ExpressionWalkerResult.Continue
                }
            }

            is ContainsElementExpression<*, *, *> -> {
                val collectionReplacement = walk(
                    context.sub(expression.collection),
                    callback
                ).replaceWith
                val elementReplacement = walk(
                    context.sub(expression.element),
                    callback
                ).replaceWith
                when {
                    collectionReplacement != null || elementReplacement != null -> ContainsElementExpression(
                        collection = (collectionReplacement
                            ?: expression.collection) as Expression<Any?, Collection<*>>,
                        element = (elementReplacement ?: expression.element) as Expression<Any?, Any?>,
                    ).let { ExpressionWalkerResult.Replace(it) }

                    else -> ExpressionWalkerResult.Continue
                }
            }

            is BinaryOperationExpression<*, *, *, *> -> {
                val leftReplacement = walk(
                    context.sub(expression.leftOperand),
                    callback
                ).replaceWith
                val rightReplacement = walk(
                    context.sub(expression.rightOperand),
                    callback
                ).replaceWith
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

            is NotExpression<*> -> walk(
                context.sub(expression.expression),
                callback
            ).replaceWith
                ?.let {
                    ExpressionWalkerResult.Replace(
                        NotExpression(
                            it as PredicateExpression<Any?>
                        )
                    )
                } ?: ExpressionWalkerResult.Continue

            is OrExpression<*> -> {
                val replacements = expression.predicates.associate {
                    it to walk(
                        context.sub(it),
                        callback
                    ).replaceWith
                }
                if (replacements.values.filterNotNull().isEmpty()) {
                    ExpressionWalkerResult.Continue
                } else {
                    val replacedPredicates = expression.predicates.map {
                        (replacements[it] ?: it) as PredicateExpression<Any?>
                    }
                    ExpressionWalkerResult.Replace(
                        OrExpression(
                            replacedPredicates
                        )
                    )
                }
            }

            is PropertyExpression<*, *, *> -> walk(
                context.sub(expression.instance),
                callback
            ).replaceWith
                ?.let {
                    ExpressionWalkerResult.Replace(
                        PropertyExpression(
                            it as Expression<Any?, Any>,
                            expression.property as KProperty1<Any, Any?>
                        )
                    )
                }
                ?: ExpressionWalkerResult.Continue

            is ConjunctionExpression<*, *>, is ConstantExpression<*, *>, is RootExpression<*> -> result
        }
    }
    
    companion object {
        /**
         * Checks if this expression has any direct child (level 1) that matches the given predicate.
         * 
         * This function only examines the immediate children of this expression and does not
         * recursively traverse nested children. For example, in a binary operation expression
         * like `property.isEqual(value)`, the direct children would be the property expression
         * and the constant value expression.
         * 
         * @param predicate The condition to check against each direct child expression
         * @return `true` if at least one direct child matches the predicate, `false` otherwise
         * 
         * @see ExpressionWalker.walk for recursive tree traversal
         */
        fun Expression<*,*>.hasAnyDirectChildThat(
            predicate: (Expression<*, *>) -> Boolean
        ): Boolean {
            var result = false
            
            ExpressionWalker().walk(
                this
            ) { currentChildContext ->
                
                when(currentChildContext.level) {
                    0 -> ExpressionWalkerResult.Continue
                    1 -> {
                        result = result || predicate(currentChildContext.expression)
                        ExpressionWalkerResult(
                            null,
                            false
                        )
                    }
                    else -> ExpressionWalkerResult(
                        null,
                        false
                    )
                }
                
            }
            
            return result
        }
    }
}