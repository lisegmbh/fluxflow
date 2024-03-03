package de.lise.fluxflow.reflection.activation.function

import de.lise.fluxflow.reflection.activation.parameter.ParameterResolution
import kotlin.reflect.KFunction

class BasicFunctionResolution<T>(
    private val function: KFunction<T>,
    private val parameters: List<ParameterResolution>
) : FunctionResolution<T> {
    override fun call(): T {
        return function.call(
            *(
                parameters.map { it.get() }
                    .toTypedArray()
            )
        )
    }
}