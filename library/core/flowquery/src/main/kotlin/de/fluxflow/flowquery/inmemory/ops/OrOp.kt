package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation

internal data class OrOp<TRoot>(
    private val conditions: List<InMemoryOperation<TRoot, Boolean>>
): InMemoryOperation<TRoot, Boolean> {
    override fun execute(input: TRoot): Boolean {
        return conditions.any {
            it.execute(input) ?: throw NullPointerException("Null returned by '$it' could not be converted to boolean.")
        }
    }

    override fun toString(): String {
        return "OR(${
            conditions.joinToString(", ")
        })"
    }
}