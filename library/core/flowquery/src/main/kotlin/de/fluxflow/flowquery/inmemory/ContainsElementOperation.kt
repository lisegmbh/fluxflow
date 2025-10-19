package de.fluxflow.flowquery.inmemory

data class ContainsElementOperation<TRoot, TCollection : Collection<TElement>, TElement>(
    private val collection: InMemoryOperation<TRoot, TCollection>,
    private val element: InMemoryOperation<TRoot, TElement>
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return collection.execute(input)?.contains(
            element.execute(input)
        )
    }
}