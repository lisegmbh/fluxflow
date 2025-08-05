package de.lise.fluxflow.reflection.activation.parameter

import de.lise.fluxflow.reflection.activation.TypeActivator
import kotlin.reflect.jvm.jvmErasure

/**
 * This resolver tries to construct parameters by instantiating them using a given [typeActivator].
 */
class InstantiatingParameterResolver(
    private val typeActivator: TypeActivator,
) : ParameterResolver {
    override fun resolveParameter(functionParam: FunctionParameter<*>): ParameterResolution? {
        return typeActivator.findActivation(functionParam.param.type.jvmErasure)?.let { activation ->
                ParameterResolution {
                    activation.activate()
                }
            }
    }
}