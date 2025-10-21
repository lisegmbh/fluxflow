package de.fluxflow.flowquery.expression.compilation

import de.fluxflow.flowquery.expression.Expression

class CompilationException : Exception {
    constructor(
        rootExpression: Expression<*, *>,
        currentExpression: Expression<*, *>
    ) : super(
        "Compilation failed for expression $currentExpression of $rootExpression."
    )

    constructor(
        rootExpression: Expression<*, *>,
        currentExpression: Expression<*, *>,
        reason: String
    ) : super(
        "Compilation failed for expression $currentExpression of $rootExpression: $reason"
    )
}