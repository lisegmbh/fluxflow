package de.fluxflow.flowquery.inmemory

fun interface InMemoryOperation<TRoot, TResult> {
    fun execute(input: TRoot): TResult?
}

