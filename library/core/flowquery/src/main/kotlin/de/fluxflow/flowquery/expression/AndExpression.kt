package de.fluxflow.flowquery.expression

class AndExpression<TRoot>(predicates: List<FlowPredicate<TRoot>>) : FlowPredicate<TRoot> {
    val predicates = predicates.simplify()

    private companion object {
        fun <TRoot> List<FlowPredicate<TRoot>>.simplify(): List<FlowPredicate<TRoot>> {
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