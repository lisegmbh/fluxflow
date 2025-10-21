package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation

data class ContainsElementThatOp<TRoot, TElement>(
    val collection: InMemoryOperation<TRoot, Collection<TElement>>,
    val predicate: InMemoryOperation<TElement, Boolean>
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean? {
        return collection.execute(input)?.any {
            predicate.execute(it) ?: return null
        }
    }
}