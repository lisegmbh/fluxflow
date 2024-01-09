package de.lise.fluxflow.reflection.activation.parameter

class FixedValueParameterResolver<T>(
    private val matcher: ValueMatcher<T>,
    private val value: T
): ParameterResolver {
    override fun resolveParameter(functionParam: FunctionParameter<*>): ParameterResolution? {
        return if (matcher.matches(functionParam, value)) {
            ParameterResolution {
                value
            }
        } else {
            null
        }
    }
}

