package de.fluxflow.flowquery.expression

class AndOperator<TRoot>(predicates: List<FlowPredicate<TRoot>>) : FlowPredicate<TRoot> {
    val predicates = predicates.simplify()

    private companion object {
        fun <TRoot> List<FlowPredicate<TRoot>>.simplify(): List<FlowPredicate<TRoot>> {
            return this.flatMap {
                when(it) {
                    is AndOperator<TRoot> -> it.predicates
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
}