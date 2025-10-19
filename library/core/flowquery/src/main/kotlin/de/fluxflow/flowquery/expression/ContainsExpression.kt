package de.fluxflow.flowquery.expression

data class ContainsExpression<TRoot>(
    val value: Expression<TRoot, String>,
    val substring: Expression<TRoot, String>,
    val ignoreCasing: Boolean
) : FlowPredicate<TRoot> {
    override fun toText(): String {
        return "(${value.toText()}).contains(${substring.toText()}, ${
            when (ignoreCasing) {
                true -> "CASE_INSENSITIVE"
                false -> "CASE_SENSITIVE"
            }
        })"
    }

    override fun toString(): String {
        return toText()
    }
}