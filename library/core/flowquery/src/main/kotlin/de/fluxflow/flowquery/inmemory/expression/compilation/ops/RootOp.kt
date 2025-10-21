package de.fluxflow.flowquery.inmemory.expression.compilation.ops

internal class RootOp<TRoot>
    : InMemoryOp<TRoot, TRoot> {

    override fun execute(input: TRoot): TRoot {
        return input
    }

    override fun toString(): String {
        return "$"
    }
}

