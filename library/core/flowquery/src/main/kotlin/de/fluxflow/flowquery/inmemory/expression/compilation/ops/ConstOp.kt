package de.fluxflow.flowquery.inmemory.expression.compilation.ops

internal data class ConstOp<TRoot,TValue>(
    val value: TValue
): InMemoryOp<TRoot, TValue> {
    override fun execute(input: TRoot): TValue {
        return value
    }

    override fun toString(): String {
        return "$value"
    }
}