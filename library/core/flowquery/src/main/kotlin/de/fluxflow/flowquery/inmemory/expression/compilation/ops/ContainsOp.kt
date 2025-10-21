package de.fluxflow.flowquery.inmemory.expression.compilation.ops

data class ContainsOp<TRoot>(
    private val value: InMemoryOp<TRoot, String>,
    private val substring: InMemoryOp<TRoot, String>,
    private val ignoreCasing: Boolean
) : InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return value.execute(input)?.indexOf(
            string = substring.execute(input) ?: return null,
            startIndex = 0,
            ignoreCase = true
        )?.let {
            it != -1
        }
    }
}