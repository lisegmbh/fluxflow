package de.fluxflow.flowquery.expression

class AndExpression<TRoot>(predicates: List<PredicateExpression<TRoot>>) : LogicalExpression<TRoot> {
    val predicates = predicates.simplify()

    private companion object {
        fun <TRoot> List<PredicateExpression<TRoot>>.simplify(): List<PredicateExpression<TRoot>> {
            return this.flatMap {
                when(it) {
                    is AndExpression<TRoot> -> it.predicates
                    else -> listOf(it)
                }
            }
        }
    }

    override fun toText(): String {
        return "AND(${predicates.joinToString(", ") { it.toText() }})"
    }

    override fun toString(): String {
        return toText()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is AndExpression<*>) {
            return false
        }
        return predicates == other.predicates
    }

    override fun hashCode(): Int {
        return predicates.hashCode()
    }
}