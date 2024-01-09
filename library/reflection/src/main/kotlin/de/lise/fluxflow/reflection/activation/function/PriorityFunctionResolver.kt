package de.lise.fluxflow.reflection.activation.function

import kotlin.reflect.KFunction

class PriorityFunctionResolver(
    private val resolvers: Collection<FunctionResolver>
) : FunctionResolver {
    override fun <TResult> resolve(function: KFunction<TResult>): FunctionResolution<TResult>? {
        return resolvers.firstNotNullOfOrNull {resolver ->
            resolver.resolve(function)
        }
    }
}