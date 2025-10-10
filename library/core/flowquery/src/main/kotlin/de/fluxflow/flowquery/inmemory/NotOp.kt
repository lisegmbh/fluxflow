package de.fluxflow.flowquery.inmemory

internal data class NotOp<TRoot>(
    private val expression: InMemoryOperation<TRoot, Boolean>
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean {
        return !(
                expression.execute(input)
                    ?: throw NullPointerException("Null returned by '$expression' could not be converted to boolean.")
                )
    }

    override fun toString(): String {
        return "NOT($expression)"
    }
}