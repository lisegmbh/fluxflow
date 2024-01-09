package de.lise.fluxflow.reflection.activation.parameter

open class CallbackParameterResolver<T>(
    private val matcher: ParamMatcher,
    private val provider: ParameterProvider<T>,
): ParameterResolver {
    override fun resolveParameter(
        functionParam: FunctionParameter<*>
    ): ParameterResolution? {
        return if (matcher.matches(functionParam)) {
            ParameterResolution {
                provider.provide()
            }
        } else {
            null
        }
    }
}