package de.fluxflow.flowquery.expression

class OrOperator<TRoot>(
    predicates: List<FlowPredicate<TRoot>>
): FlowPredicate<TRoot> {
    val predicates = predicates.simplify()

    private companion object {
        private fun <TRoot> List<FlowPredicate<TRoot>>.simplify(): List<FlowPredicate<TRoot>> {
            return this.flatMap {
                when(it) {
                    is OrOperator<TRoot> -> it.predicates
                    else -> listOf(it)
                }
            }
        }
    }

    override fun toText(): String {
        return "OR(${predicates.joinToString(", "){ it.toText() }})"
    }

    override fun toString(): String {
        return toText()
    }

    override fun equals(other: Any?): Boolean {
        if(other !is OrOperator<*>) {
            return false
        }
        return predicates == other.predicates
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}