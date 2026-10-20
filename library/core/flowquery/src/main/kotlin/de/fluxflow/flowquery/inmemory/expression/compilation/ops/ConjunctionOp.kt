package de.fluxflow.flowquery.inmemory.expression.compilation.ops

internal class ConjunctionOp<TRoot, TCurrent> : InMemoryOp<TRoot, TCurrent> {
    override fun execute(input: TRoot): TCurrent? {
        return input as? TCurrent
    }

    override fun toString(): String {
        return "$"
    }
}