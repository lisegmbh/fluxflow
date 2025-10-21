package de.fluxflow.flowquery.inmemory

import de.fluxflow.flowquery.expression.*
import de.fluxflow.flowquery.inmemory.ops.*
import de.fluxflow.flowquery.inmemory.query.sorting.InMemoryComparator
import kotlin.reflect.KProperty1

class InMemoryCompiler : ExpressionCompiler<InMemoryOperation<*, *>> {
    override fun <TRoot, TResult> compile(
        expression: Expression<TRoot, TResult>
    ): CompilationResult<InMemoryOperation<TRoot, TResult>> {
        return CompilationResult(
            doCompile(expression, expression)
        )
    }

    private fun <TRoot, TResult> doCompile(
        rootExpression: Expression<*, *>,
        exp: Expression<TRoot, TResult>
    ): InMemoryOperation<TRoot, TResult> {
        return when (exp) {
            is Root<*> -> RootOp()
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

            is NotOperator<TRoot> -> {
                val expression = doCompile(rootExpression, exp.expression)
                NotOp(
                    expression
                )
            }

            is BinaryOperationExpression<TRoot, *, *, *> -> {
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
                    exp.anyOf
                )
            }

            is PropertyExpression<TRoot, *, *> -> {
                val instanceGetter = doCompile(rootExpression, exp.instance)
                PropertyOp(
                    instanceGetter as InMemoryOperation<TRoot, Any?>,
                    exp.property as KProperty1<Any, Any?>
                )
            }

            is Constant<*, *> -> ConstOp(exp.value)
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
                ) as InMemoryOperation<TRoot, Collection<*>>,
                doCompile(
                    rootExpression,
                    exp.elementPredicate
                ) as InMemoryOperation<Any?, Boolean>
            )

            is ContainsElementExpression<TRoot, *, *> -> ContainsElementOp(
                doCompile(
                    rootExpression,
                    exp.collection
                ) as InMemoryOperation<TRoot, Collection<*>>,
                doCompile(
                    rootExpression,
                    exp.element
                ) as InMemoryOperation<TRoot, Any?>
            )

            is IsTypeExpression<TRoot, *, *> -> IsTypeOp(
                doCompile(
                    rootExpression,
                    exp.instance
                ) as InMemoryOperation<TRoot, Any?>,
                exp.requiredType
            )

            is CastExpression<TRoot, *, *> -> CastOp(
                doCompile(
                    rootExpression,
                    exp.instance
                ) as InMemoryOperation<TRoot, Any?>,
                exp.requiredType
            )

            is MapAccessExpression<TRoot, *, *, *> -> InMemoryMapAccessOp(
                doCompile(
                    rootExpression,
                    exp.instance
                ) as InMemoryOperation<TRoot, Map<Any?, Any?>>,
                doCompile(
                    rootExpression,
                    exp.key
                ) as InMemoryOperation<TRoot, Any?>
            )

        } as InMemoryOperation<TRoot, TResult>
    }
}

