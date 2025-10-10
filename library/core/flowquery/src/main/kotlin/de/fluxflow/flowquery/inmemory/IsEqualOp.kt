package de.fluxflow.flowquery.inmemory

internal data class IsEqualOp<TRoot>(
    private val operator1: InMemoryOperation<TRoot, *>,
    private val operator2: InMemoryOperation<TRoot, *>
): InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean {
        return operator1.execute(input) == operator2.execute(input)
    }

    override fun toString(): String {
        return "$operator1 == $operator2"
    }
}