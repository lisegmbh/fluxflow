package de.fluxflow.flowquery.inmemory.expression.compilation.ops

data class ContainsElementThatOp<TRoot, TElement>(
    val collection: InMemoryOp<TRoot, Collection<TElement>>,
    val predicate: InMemoryOp<TElement, Boolean>
) : InMemoryOp<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return collection.execute(input)?.any {
            predicate.execute(it) ?: return null
        }
    }
}