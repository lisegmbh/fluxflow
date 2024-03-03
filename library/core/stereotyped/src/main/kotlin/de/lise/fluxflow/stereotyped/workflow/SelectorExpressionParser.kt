package de.lise.fluxflow.stereotyped.workflow

/**
 * The [SelectorExpressionParser] parses the [ModelListener.selector] expression into an executable [SelectorExpression].
 */
interface SelectorExpressionParser {
    /**
     * Parses the given [expression] into a [SelectorExpression].
     * @param expression The expression to be parsed.
     * @return The parsed expression.
     */
    fun parse(expression: String): SelectorExpression
}