package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation

data class InMemoryMapAccessOp<TRoot, TCurrent : Map<TKey, TValue>, TKey, TValue>(
    private val instance: InMemoryOperation<TRoot, TCurrent>,
    private val key: InMemoryOperation<TRoot, TKey>,
) : InMemoryOperation<TRoot, TValue> {
    override fun execute(input: TRoot): TValue? {
        val key = key.execute(input) ?: return null
        val instance = instance.execute(input) ?: return null

        return instance[key]
    }
}