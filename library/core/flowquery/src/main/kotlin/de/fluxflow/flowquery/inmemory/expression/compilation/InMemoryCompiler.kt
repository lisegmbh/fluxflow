package de.fluxflow.flowquery.inmemory.expression.compilation

import de.fluxflow.flowquery.expression.*
import de.fluxflow.flowquery.expression.compilation.CompilationException
import de.fluxflow.flowquery.expression.compilation.CompilationResult
import de.fluxflow.flowquery.expression.compilation.ExpressionCompiler
import de.fluxflow.flowquery.inmemory.expression.compilation.ops.*
import de.fluxflow.flowquery.inmemory.query.sorting.InMemoryComparator
import kotlin.reflect.KProperty1

class InMemoryCompiler : ExpressionCompiler<InMemoryOp<*, *>> {
    override fun <TRoot, TResult> compile(
        expression: Expression<TRoot, TResult>
    ): CompilationResult<InMemoryOp<TRoot, TResult>> {
        return CompilationResult(
            doCompile(
                expression,
                expression
            )
        )
    }

    private fun <TRoot, TResult> doCompile(
        rootExpression: Expression<*, *>,
        exp: Expression<TRoot, TResult>
    ): InMemoryOp<TRoot, TResult> {
        return when (exp) {
            is RootExpression<*> -> RootOp()
            is AndExpression<TRoot> -> {
                val conditions = exp.predicates.map { predicate ->
                    doCompile(rootExpression, predicate)
                }
                AndOp(conditions)
            }

            is OrExpression<TRoot> -> {
                val conditions = exp.predicates.map { predicate ->
                    doCompile(rootExpression, predicate)
                }
                OrOp(conditions)
            }

            is NotExpression<TRoot> -> {
                val expression = doCompile(rootExpression, exp.expression)
                NotOp(
                    expression
                )
            }

             is BinaryOperationExpression<TRoot, *, *> -> {
                when (exp.operation) {
                    else -> BinaryOperatorOp(
                        when (exp.operation) {
                            BinaryOperation.Equal -> BinaryOperatorOp.Operation("=") { a, b ->
                                a == b
                            }

                            BinaryOperation.NotEqual -> BinaryOperatorOp.Operation("!=") { a, b ->
                                a != b
                            }

                            BinaryOperation.LessThan -> BinaryOperatorOp.Operation("<") { a, b ->
                                InMemoryComparator().compare(
                                    a,
                                    b
                                ) < 0
                            }

                            BinaryOperation.LessThanOrEqual -> BinaryOperatorOp.Operation("<=") { a, b ->
                                InMemoryComparator().compare(
                                    a,
                                    b
                                ) <= 0
                            }

                            BinaryOperation.GreaterThan -> BinaryOperatorOp.Operation(">") { a, b ->
                                InMemoryComparator().compare(
                                    a,
                                    b
                                ) > 0
                            }

                            BinaryOperation.GreaterThanOrEqual -> BinaryOperatorOp.Operation(">=") { a, b ->
                                InMemoryComparator().compare(
                                    a,
                                    b
                                ) >= 0
                            }

                            else -> throw CompilationException(
                                rootExpression,
                                exp
                            )
                        },
                        doCompile(
                            rootExpression,
                            exp.leftOperand
                        ),
                        doCompile(
                            rootExpression,
                            exp.rightOperand
                        )
                    )
                }
            }

            is IsAnyOfOperator<TRoot, *> -> {
                val valueAccessor = doCompile(rootExpression, exp.valueToTest)
                IsAnyOfOp(
                    valueAccessor,
                    exp.anyOf.map {
                        doCompile(rootExpression, it) as InMemoryOp<TRoot, *>
                    }.toSet()
                )
            }

            is PropertyExpression<TRoot, *, *> -> {
                val instanceGetter = doCompile(rootExpression, exp.instance)
                PropertyOp(
                    instanceGetter as InMemoryOp<TRoot, Any?>,
                    exp.property as KProperty1<Any, Any?>
                )
            }

            is ConstantExpression<*, *> -> ConstOp(exp.value)
            is ConjunctionExpression<*, *> -> ConjunctionOp()
            is StartsWithExpression<TRoot> -> {
                StartsWithOp(
                    doCompile(
                        rootExpression,
                        exp.value
                    ),
                    doCompile(
                        rootExpression,
                        exp.prefix
                    ),
                    exp.ignoreCasing
                )
            }
            is EndsWithExpression<TRoot> -> {
                EndsWithOp(
                    doCompile(
                        rootExpression,
                        exp.value
                    ),
                    doCompile(
                        rootExpression,
                        exp.suffix
                    ),
                    exp.ignoreCasing
                )
            }
            is ContainsExpression<TRoot> -> ContainsOp(
                doCompile(
                    rootExpression,
                    exp.value
                ),
                doCompile(
                    rootExpression,
                    exp.substring
                ),
                exp.ignoreCasing
            )

            is ContainsElementThatExpression<TRoot, *, *> -> ContainsElementThatOp(
                doCompile(
                    rootExpression,
                    exp.collection
                ) as InMemoryOp<TRoot, Collection<*>>,
                doCompile(
                    rootExpression,
                    exp.elementPredicate
                ) as InMemoryOp<Any?, Boolean>
            )

            is ContainsElementExpression<TRoot, *, *> -> ContainsElementOp(
                doCompile(
                    rootExpression,
                    exp.collection
                ) as InMemoryOp<TRoot, Collection<*>>,
                doCompile(
                    rootExpression,
                    exp.element
                ) as InMemoryOp<TRoot, Any?>
            )

            is IsTypeExpression<TRoot, *, *> -> IsTypeOp(
                doCompile(
                    rootExpression,
                    exp.instance
                ) as InMemoryOp<TRoot, Any?>,
                exp.requiredType
            )

            is CastExpression<TRoot, *, *> -> CastOp(
                doCompile(
                    rootExpression,
                    exp.instance
                ) as InMemoryOp<TRoot, Any?>,
                exp.requiredType
            )

            is MapAccessExpression<TRoot, *, *, *> -> InMemoryMapAccessOp(
                doCompile(
                    rootExpression,
                    exp.instance
                ) as InMemoryOp<TRoot, Map<Any?, Any?>>,
                doCompile(
                    rootExpression,
                    exp.key
                ) as InMemoryOp<TRoot, Any?>
            )
            
            is HasKeyExpression<TRoot, *> -> InMemoryHasKeyOp(
                doCompile(
                    rootExpression,
                    exp.instance
                ) as InMemoryOp<TRoot, Map<Any?, Any?>>,
                doCompile(
                    rootExpression,
                    exp.key
                ) as InMemoryOp<TRoot, Any?>
            )

        } as InMemoryOp<TRoot, TResult>
    }
}

