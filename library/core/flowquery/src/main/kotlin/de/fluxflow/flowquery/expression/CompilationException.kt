package de.fluxflow.flowquery.expression

class CompilationException : Exception {
    constructor(
        rootExpression: Expression<*,*>,
        currentExpression: Expression<*,*>
    ) : super(
        "Compilation failed for expression $currentExpression of $rootExpression."
    )

    constructor(
        rootExpression: Expression<*,*>,
        currentExpression: Expression<*,*>,
        reason: String
    ) : super(
        "Compilation failed for expression $currentExpression of $rootExpression: $reason"
    )
}