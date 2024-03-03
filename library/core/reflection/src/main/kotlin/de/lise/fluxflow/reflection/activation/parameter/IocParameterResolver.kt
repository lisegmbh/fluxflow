package de.lise.fluxflow.reflection.activation.parameter

import de.lise.fluxflow.api.ioc.IocProvider

class IocParameterResolver(
    private val iocProvider: IocProvider
) : ParameterResolver {
    override fun resolveParameter(functionParam: FunctionParameter<*>): ParameterResolution? {
        return iocProvider.provide(functionParam.param.type)?.let { resolvedValue ->
            ParameterResolution { resolvedValue }
        }
    }
}