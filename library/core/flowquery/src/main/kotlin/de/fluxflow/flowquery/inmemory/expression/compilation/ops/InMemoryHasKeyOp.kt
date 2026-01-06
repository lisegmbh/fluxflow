package de.fluxflow.flowquery.inmemory.expression.compilation.ops

class InMemoryHasKeyOp<TRoot, TKey>(
    private val instance: InMemoryOp<TRoot, Map<TKey,*>>,
    private val key: InMemoryOp<TRoot, TKey>
): InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        val key = key.execute(input) ?: return null
        return instance.execute(input)
            ?.containsKey(key)
    }
}