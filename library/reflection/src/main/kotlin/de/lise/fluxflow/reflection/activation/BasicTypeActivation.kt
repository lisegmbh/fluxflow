package de.lise.fluxflow.reflection.activation

import de.lise.fluxflow.reflection.activation.function.FunctionResolution

class BasicTypeActivation<T>(
    private val constructorResolution: FunctionResolution<T>
) : TypeActivation<T> {
    override fun activate(): T {
        return constructorResolution.call()
    }
}