package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation

data class StartsWithOp<TRoot>(
    private val value: InMemoryOperation<TRoot, String>,
    private val prefix: InMemoryOperation<TRoot, String>,
    private val ignoreCasing: Boolean
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
       return value.execute(input)?.startsWith(
           prefix = prefix.execute(input) ?: return null,
           ignoreCase = ignoreCasing
       )
    }
}