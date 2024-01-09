package de.lise.fluxflow.reflection.activation.function

import de.lise.fluxflow.reflection.activation.parameter.FunctionParameter
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import kotlin.reflect.KFunction

class BasicFunctionResolver(
    private val parameterResolver: ParameterResolver
) : FunctionResolver {
    override fun <TResult> resolve(function: KFunction<TResult>): FunctionResolution<TResult>? {
        val parameters = function.parameters
            .map { FunctionParameter(function, it) }
            .map {parameterResolver.resolveParameter(it) }

        if (parameters.any { it == null }) {
            return null
        }
        return BasicFunctionResolution(
            function,
            parameters.filterNotNull()
        )
    }
}