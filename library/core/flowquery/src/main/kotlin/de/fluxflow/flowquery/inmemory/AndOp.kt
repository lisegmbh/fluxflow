package de.fluxflow.flowquery.inmemory

internal data class AndOp<TRoot>(
    private val conditions: List<InMemoryOperation<TRoot, Boolean>>
) : InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean {
        return conditions.all {
            it.execute(input) ?: throw NullPointerException("Null returned by '$it' could not be cast to boolean.")
        }
    }

    override fun toString(): String {
        return "AND(${
            conditions.joinToString(", ")
        })"
    }
}

