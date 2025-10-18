package de.fluxflow.flowquery.inmemory

internal class ConjunctionOp<TRoot, TCurrent> : InMemoryOperation<TRoot, TCurrent> {
    override fun execute(input: TRoot): TCurrent? {
        return input as? TCurrent
    }

    override fun toString(): String {
        return "$"
    }
}