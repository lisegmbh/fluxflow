package de.fluxflow.flowquery.inmemory

internal data class IsAnyOfOp<TRoot>(
    private val expression: InMemoryOperation<TRoot, *>,
    private val values: Set<*>
): InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean {
        return values.contains(expression.execute(input))
    }

    override fun toString(): String {
        return "$expression IN (${values.joinToString(", ")})"
    }
}