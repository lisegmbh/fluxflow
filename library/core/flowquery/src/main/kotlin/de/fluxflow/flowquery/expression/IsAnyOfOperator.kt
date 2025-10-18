package de.fluxflow.flowquery.expression

data class IsAnyOfOperator<TRoot, T>(
    val valueToTest: Expression<TRoot, T>,
    val anyOf: Set<T>
): FlowPredicate<TRoot> {
    override fun toText(): String {
        return "${valueToTest.toText()} IN (${anyOf.joinToString(", ")})"
    }

    override fun toString(): String {
        return toText()
    }
}