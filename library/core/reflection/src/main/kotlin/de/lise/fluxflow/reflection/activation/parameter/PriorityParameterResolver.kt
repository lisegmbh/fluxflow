package de.lise.fluxflow.reflection.activation.parameter

/**
 * This parameter returns the first non-`null` result returned by the nested resolvers.
 * @param resolvers The resolvers to be used to resolve a given parameter.
 */
class PriorityParameterResolver(
    private val resolvers: Collection<ParameterResolver>
) : ParameterResolver {
    constructor(vararg resolvers: ParameterResolver) : this(listOf(*resolvers))

    override fun resolveParameter(
        functionParam: FunctionParameter<*>
    ): ParameterResolution? {
        return resolvers.firstNotNullOfOrNull { resolver ->
            resolver.resolveParameter(functionParam)
        }
    }
}