package de.fluxflow.flowquery.inmemory

import de.fluxflow.flowquery.expression.*
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
        return when(exp) {
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
            is IsEqual<TRoot, *, *> -> {
                val res1 = doCompile(rootExpression, exp.leftSide)
                val res2 = doCompile(rootExpression, exp.rightSide)
                IsEqualOp(res1, res2)
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
            else -> throw CompilationException(rootExpression, exp)
        } as InMemoryOperation<TRoot, TResult>
    }
}