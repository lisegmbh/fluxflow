package de.lise.fluxflow.reflection.activation.parameter


fun interface ParameterResolver {
    fun resolveParameter(
        functionParam: FunctionParameter<*>
    ): ParameterResolution?
}