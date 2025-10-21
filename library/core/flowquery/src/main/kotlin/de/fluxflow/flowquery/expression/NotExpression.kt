package de.fluxflow.flowquery.expression

class NotExpression<TRoot>(
    expression: PredicateExpression<TRoot>
): PredicateExpression<TRoot> {
    val expression: PredicateExpression<TRoot> = simplify(expression)

    private companion object {
        fun <TRoot> simplify(expression: PredicateExpression<TRoot>): PredicateExpression<TRoot> {
            return when(expression) {
                is NotExpression -> expression.expression
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
        if(other !is NotExpression<*>) {
            return false
        }
        return expression == other.expression
    }

    override fun hashCode(): Int {
        return expression.hashCode()
    }
}