package de.fluxflow.flowquery.inmemory.expression.compilation.ops

data class InMemoryMapAccessOp<TRoot, TCurrent : Map<TKey, TValue>, TKey, TValue>(
    private val instance: InMemoryOp<TRoot, TCurrent>,
    private val key: InMemoryOp<TRoot, TKey>,
) : InMemoryOp<TRoot, TValue> {
    override fun execute(input: TRoot): TValue? {
        val key = key.execute(input) ?: return null
        val instance = instance.execute(input) ?: return null

        return instance[key]
    }
}