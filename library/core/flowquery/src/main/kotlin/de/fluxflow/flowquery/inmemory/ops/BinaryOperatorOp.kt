package de.fluxflow.flowquery.inmemory.ops

import de.fluxflow.flowquery.inmemory.InMemoryOperation

internal data class BinaryOperatorOp<TRoot, TElement, TOtherElement, TResult>(
    private val operation: Operation<TElement, TOtherElement, TResult>,
    private val operator1: InMemoryOperation<TRoot, TElement>,
    private val operator2: InMemoryOperation<TRoot, TOtherElement>,
): InMemoryOperation<TRoot, TResult> {

    override fun execute(input: TRoot): TResult? {
        return operation.implementation(
            operator1.execute(input),
            operator2.execute(input)
        )
    }

    override fun toString(): String {
        return "$operator1 ${operation.symbol} $operator2"
    }


    data class Operation<TElement, TOtherElement, TResult>(
        val symbol: String,
        val implementation: (a: TElement?, b: TOtherElement?) -> TResult
    )

}