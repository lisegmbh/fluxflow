package de.fluxflow.flowquery.expression

class IsAnyOfOperator<TRoot, T>(
    val valueToTest: Expression<TRoot, T>,
    val anyOf: Set<T>
): FlowPredicate<TRoot> {
    override fun asText(): String {
        return "${valueToTest.asText()} IN (${anyOf.joinToString(", ")})"
    }

    override fun toString(): String {
        return asText()
    }
}