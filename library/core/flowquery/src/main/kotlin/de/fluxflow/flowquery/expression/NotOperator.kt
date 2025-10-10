package de.fluxflow.flowquery.expression

class NotOperator<TRoot>(
    expression: FlowPredicate<TRoot>
): FlowPredicate<TRoot> {
    val expression: FlowPredicate<TRoot> = simplify(expression)

    private companion object {
        fun <TRoot> simplify(expression: FlowPredicate<TRoot>): FlowPredicate<TRoot> {
            return when(expression) {
                is NotOperator -> expression.expression
                else -> expression
            }
        }
    }

    override fun asText(): String {
        return "NOT(${expression.asText()})"
    }

    override fun toString(): String {
        return asText()
    }
}