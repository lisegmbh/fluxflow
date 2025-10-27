package de.fluxflow.flowquery.inmemory.expression.compilation.ops

internal data class AndOp<TRoot>(
    private val conditions: List<InMemoryOp<TRoot, Boolean>>
) : InMemoryOp<TRoot, Boolean> {
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

