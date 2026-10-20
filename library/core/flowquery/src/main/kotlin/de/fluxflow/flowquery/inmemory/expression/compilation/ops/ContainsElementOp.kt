package de.fluxflow.flowquery.inmemory.expression.compilation.ops

data class ContainsElementOp<TRoot, TCollection : Collection<TElement>, TElement>(
    private val collection: InMemoryOp<TRoot, TCollection>,
    private val element: InMemoryOp<TRoot, TElement>
) : InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return collection.execute(input)?.contains(
            element.execute(input)
        )
    }
}