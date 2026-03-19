package de.fluxflow.flowquery.inmemory.expression.compilation.ops

internal data class IsAnyOfOp<TRoot>(
    private val expression: InMemoryOp<TRoot, *>,
    private val values: Set<InMemoryOp<TRoot, *>>
): InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean {
        return values
            .map { it.execute(input) }
            .toSet()
            .contains(expression.execute(input))
    }

    override fun toString(): String {
        return "$expression IN (${values.joinToString(", ")})"
    }
}