package de.fluxflow.flowquery.inmemory.expression.compilation.ops

fun interface InMemoryOp<TRoot, TResult> {
    fun execute(input: TRoot): TResult?
}