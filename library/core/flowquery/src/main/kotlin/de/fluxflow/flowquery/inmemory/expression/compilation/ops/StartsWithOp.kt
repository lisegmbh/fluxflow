package de.fluxflow.flowquery.inmemory.expression.compilation.ops

data class StartsWithOp<TRoot>(
    private val value: InMemoryOp<TRoot, String>,
    private val prefix: InMemoryOp<TRoot, String>,
    private val ignoreCasing: Boolean
) : InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
       return value.execute(input)?.startsWith(
           prefix = prefix.execute(input) ?: return null,
           ignoreCase = ignoreCasing
       )
    }
}