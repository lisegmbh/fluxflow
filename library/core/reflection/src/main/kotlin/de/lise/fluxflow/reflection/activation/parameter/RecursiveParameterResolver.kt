package de.lise.fluxflow.reflection.activation.parameter

import de.lise.fluxflow.reflection.activation.TypeActivator
import kotlin.reflect.KClass

class RecursiveParameterResolver(
    private val typeResolver: TypeActivator,
) : ParameterResolver {
    override fun resolveParameter(functionParam: FunctionParameter<*>): ParameterResolution? {
        val parameterClass = (functionParam.param.type.classifier as? KClass<*>) ?: return null
        val activation = typeResolver.findActivation(parameterClass) ?: return null

        return ParameterResolution {
            activation.activate()
        }
    }
}