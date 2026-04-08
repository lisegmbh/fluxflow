package de.fluxflow.flowquery.expression

data class IsAnyOfOperator<TRoot, T>(
    val valueToTest: Expression<TRoot, T>,
    val anyOf: Set<Expression<*,T>>
): LogicalExpression<TRoot> {
    override fun toText(): String {
        return "${valueToTest.toText()} IN (${anyOf.joinToString(", "){ it.toText() }})"
    }

    override fun toString(): String {
        return toText()
    }
}