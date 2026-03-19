package de.lise.fluxflow.springboot.rest.filter

import org.antlr.v4.runtime.*

class ExpressionParsingException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(
        offendingToken: Token,
        message: String
    ) : this(
        "Failed to process token '${offendingToken.text}' " +
                "at line ${offendingToken.line}, column ${offendingToken.charPositionInLine}: $message"
    )

    constructor(
        offendingContext: RuleContext,
        message: String
    ) : this(
        "Failed to process '${offendingContext.text}': $message"
    )
}

