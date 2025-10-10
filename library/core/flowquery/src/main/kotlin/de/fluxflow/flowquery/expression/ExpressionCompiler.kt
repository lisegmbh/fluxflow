package de.fluxflow.flowquery.expression

interface ExpressionCompiler<TCompilationResult> {
    fun <TRoot, TResult> compile(expression: Expression<TRoot, TResult>): CompilationResult<TCompilationResult>
}


