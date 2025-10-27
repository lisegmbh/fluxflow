package de.fluxflow.flowquery.expression

data class EndsWithExpression<TRoot>(
    val value: Expression<TRoot, String>,
    val suffix: Expression<TRoot, String>,
    val ignoreCasing: Boolean
) : PredicateExpression<TRoot> {
    override fun toText(): String {
        return "(${value.toText()}).endsWith(${suffix.toText()}, ${
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