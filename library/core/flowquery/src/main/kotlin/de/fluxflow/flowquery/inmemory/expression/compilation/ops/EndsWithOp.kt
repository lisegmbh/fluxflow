package de.fluxflow.flowquery.inmemory.expression.compilation.ops

data class EndsWithOp<TRoot>(
    private val value: InMemoryOp<TRoot, String>,
    private val suffix: InMemoryOp<TRoot, String>,
    private val ignoreCasing: Boolean
) : InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return value.execute(input)?.endsWith(
            suffix = suffix.execute(input) ?: return null,
            ignoreCase = ignoreCasing
        )
    }
}

