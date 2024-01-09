package de.lise.fluxflow.reflection.activation.parameter

fun interface ParameterProvider<T> {
    fun provide(): T
}

fun <TSource, TTarget> ParameterProvider<TSource>.map(mapping: (element: TSource) -> TTarget): ParameterProvider<TTarget> {
    return ParameterProvider {
        mapping(this.provide())
    }
}