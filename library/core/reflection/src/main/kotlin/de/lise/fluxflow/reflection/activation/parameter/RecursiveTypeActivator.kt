package de.lise.fluxflow.reflection.activation.parameter

import de.lise.fluxflow.reflection.activation.BasicTypeActivator
import de.lise.fluxflow.reflection.activation.TypeActivation
import de.lise.fluxflow.reflection.activation.TypeActivator
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import kotlin.reflect.KClass

/**
 * The [RecursiveTypeActivator] uses the given parameter resolvers
 * and tries to create the type using pre-existing parameter values.
 * 
 * If a parameter value can not be found,
 * it tries to activate the parameter types itself.
 */
class RecursiveTypeActivator(vararg parameterResolvers: ParameterResolver) : TypeActivator {
    private val wrappedTypeActivator: TypeActivator

    init {
        val allParameterResolvers = mutableListOf(*parameterResolvers)
        wrappedTypeActivator = BasicTypeActivator(
            BasicFunctionResolver(
                PriorityParameterResolver(
                    allParameterResolvers
                )
            )
        )
        allParameterResolvers.add(
            InstantiatingParameterResolver(
                wrappedTypeActivator
            )
        )
    }
    
    override fun <T : Any> findActivation(type: KClass<T>): TypeActivation<T>? {
        return wrappedTypeActivator.findActivation(type)
    }
}