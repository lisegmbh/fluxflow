package de.lise.fluxflow.reflection.activation

fun interface TypeActivation<T> {
    fun activate(): T
}