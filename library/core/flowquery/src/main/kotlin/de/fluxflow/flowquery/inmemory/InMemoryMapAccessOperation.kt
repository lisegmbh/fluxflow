package de.fluxflow.flowquery.inmemory

data class InMemoryMapAccessOperation<TRoot, TCurrent : Map<TKey, TValue>, TKey, TValue>(
    private val instance: InMemoryOperation<TRoot, TCurrent>,
    private val key: InMemoryOperation<TRoot, TKey>,
) : InMemoryOperation<TRoot, TValue> {
    override fun execute(input: TRoot): TValue? {
        val key = key.execute(input) ?: return null
        val instance = instance.execute(input) ?: return null

        return instance[key]
    }
}