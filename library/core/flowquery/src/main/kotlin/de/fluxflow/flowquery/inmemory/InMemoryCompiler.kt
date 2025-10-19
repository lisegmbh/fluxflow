package de.fluxflow.flowquery.inmemory

import de.fluxflow.flowquery.expression.*
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
            is AndOperator<TRoot> -> {
                val conditions = exp.predicates.map { predicate ->
                    doCompile(rootExpression, predicate)
                }
                AndOp(conditions)
            }

            is OrOperator<TRoot> -> {
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
                                InMemoryComparator().compare(a, b) < 0
                            }

                            BinaryOperation.LessThanOrEqual -> BinaryOperatorOp.Operation("<=") { a, b ->
                                InMemoryComparator().compare(a, b) <= 0
                            }

                            BinaryOperation.GreaterThan -> BinaryOperatorOp.Operation(">") { a, b ->
                                InMemoryComparator().compare(a, b) > 0
                            }

                            BinaryOperation.GreaterThanOrEqual -> BinaryOperatorOp.Operation(">=") { a, b ->
                                InMemoryComparator().compare(a, b) >= 0
                            }

                            else -> throw CompilationException(rootExpression, exp)
                        },
                        doCompile(rootExpression, exp.leftOperand),
                        doCompile(rootExpression, exp.rightOperand)
                    )
                }
            }

            is IsAnyOfOperator<TRoot, *> -> {
                val valueAccessor = doCompile(rootExpression, exp.valueToTest)
                IsAnyOfOp(valueAccessor, exp.anyOf)
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
                StartsWithOperator(
                    doCompile(rootExpression, exp.value),
                    doCompile(rootExpression, exp.prefix),
                    exp.ignoreCasing
                )
            }
            is EndsWithExpression<TRoot> -> {
                EndsWithOperator(
                    doCompile(rootExpression, exp.value),
                    doCompile(rootExpression, exp.suffix),
                    exp.ignoreCasing
                )
            }
            is ContainsExpression<TRoot> -> ContainsOperator(
                doCompile(rootExpression, exp.value),
                doCompile(rootExpression, exp.substring),
                exp.ignoreCasing
            )

            is ContainsElementThatExpression<TRoot, *, *> -> ContainsElementThatOperation(
                doCompile(rootExpression, exp.collection) as InMemoryOperation<TRoot, Collection<*>>,
                doCompile(rootExpression, exp.elementPredicate) as InMemoryOperation<Any?, Boolean>
            )
        } as InMemoryOperation<TRoot, TResult>
    }
}