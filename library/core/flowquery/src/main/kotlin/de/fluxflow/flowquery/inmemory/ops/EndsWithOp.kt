package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation

data class EndsWithOp<TRoot>(
    private val value: InMemoryOperation<TRoot, String>,
    private val suffix: InMemoryOperation<TRoot, String>,
    private val ignoreCasing: Boolean
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return value.execute(input)?.endsWith(
            suffix = suffix.execute(input) ?: return null,
            ignoreCase = ignoreCasing
        )
    }
}

