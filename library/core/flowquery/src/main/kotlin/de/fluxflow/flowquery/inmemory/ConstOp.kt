package de.fluxflow.flowquery.inmemory

internal data class ConstOp<TRoot,TValue>(
    val value: TValue
): InMemoryOperation<TRoot, TValue> {
    override fun execute(input: TRoot): TValue {
        return value
    }

    override fun toString(): String {
        return "$value"
    }
}