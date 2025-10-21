package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation

internal class RootOp<TRoot>
    : InMemoryOperation<TRoot, TRoot> {

    override fun execute(input: TRoot): TRoot {
        return input
    }

    override fun toString(): String {
        return "$"
    }
}

