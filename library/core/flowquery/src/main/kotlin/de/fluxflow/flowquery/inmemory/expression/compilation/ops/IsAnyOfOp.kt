package de.fluxflow.flowquery.inmemory.expression.compilation.ops

internal data class IsAnyOfOp<TRoot>(
    private val expression: InMemoryOp<TRoot, *>,
    private val candidates: List<InMemoryOp<TRoot, *>>
): InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean {
        val value = expression.execute(input)
        return candidates.any { it.execute(input) == value }
    }

    override fun toString(): String {
        return "$expression IN (${candidates.joinToString(", ")})"
    }
}
