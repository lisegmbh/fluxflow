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

    override fun asText(): String {
        return "AND(${predicates.joinToString(", ") { it.asText() }})"
    }

    override fun toString(): String {
        return asText()
    }
}