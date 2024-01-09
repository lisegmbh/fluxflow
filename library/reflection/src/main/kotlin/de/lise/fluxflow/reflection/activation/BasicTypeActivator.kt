package de.lise.fluxflow.reflection.activation

import de.lise.fluxflow.reflection.activation.function.FunctionResolver
import kotlin.reflect.KClass

class BasicTypeActivator(
    private val constructorResolver: FunctionResolver
) : TypeActivator {
    override fun <T : Any> findActivation(type: KClass<T>): TypeActivation<T>? {
        return type.constructors.sortedBy { constructor ->
            constructor.parameters.size
        }.firstNotNullOfOrNull { constructor ->
            constructorResolver.resolve(constructor)
        }?.let { constructorResolution ->
            BasicTypeActivation(constructorResolution)
        }
    }
}