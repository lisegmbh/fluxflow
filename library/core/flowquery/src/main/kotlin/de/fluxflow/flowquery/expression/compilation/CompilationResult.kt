package de.fluxflow.flowquery.expression.compilation

data class CompilationResult<out TCompilationResult>(
    val result: TCompilationResult
)