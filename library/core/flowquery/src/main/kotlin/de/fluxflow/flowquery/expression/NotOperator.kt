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

    override fun toText(): String {
        return "NOT(${expression.toText()})"
    }

    override fun toString(): String {
        return toText()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is NotOperator<*>) {
            return false
        }
        return expression == other.expression
    }

    override fun hashCode(): Int {
        return expression.hashCode()
    }
}