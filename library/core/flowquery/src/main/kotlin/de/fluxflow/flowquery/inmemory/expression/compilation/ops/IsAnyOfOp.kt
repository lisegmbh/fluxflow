package de.fluxflow.flowquery.inmemory.expression.compilation.ops

internal data class IsAnyOfOp<TRoot>(
    private val expression: InMemoryOp<TRoot, *>,
    private val values: Set<*>
): InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean {
        return values.contains(expression.execute(input))
    }

    override fun toString(): String {
        return "$expression IN (${values.joinToString(", ")})"
    }
}