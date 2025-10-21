package de.fluxflow.flowquery.expression.compilation

import de.fluxflow.flowquery.expression.Expression

interface ExpressionCompiler<TCompilationResult> {
    fun <TRoot, TResult> compile(expression: Expression<TRoot, TResult>): CompilationResult<TCompilationResult>
}


