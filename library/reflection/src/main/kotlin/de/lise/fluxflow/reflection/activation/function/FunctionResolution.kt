package de.lise.fluxflow.reflection.activation.function

fun interface FunctionResolution<T> {
    fun call(): T
}