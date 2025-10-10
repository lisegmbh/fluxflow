package de.fluxflow.flowquery.inmemory

internal class RootOp<TRoot>
    : InMemoryOperation<TRoot, TRoot> {

    override fun execute(input: TRoot): TRoot {
        return input
    }

    override fun toString(): String {
        return "$"
    }
}