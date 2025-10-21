package de.fluxflow.flowquery.inmemory.expression.compilation.ops

internal data class BinaryOperatorOp<TRoot, TElement, TOtherElement, TResult>(
    private val operation: Operation<TElement, TOtherElement, TResult>,
    private val operator1: InMemoryOp<TRoot, TElement>,
    private val operator2: InMemoryOp<TRoot, TOtherElement>,
): InMemoryOp<TRoot, TResult> {

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