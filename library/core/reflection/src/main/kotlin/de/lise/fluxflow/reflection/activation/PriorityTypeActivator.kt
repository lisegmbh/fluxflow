package de.lise.fluxflow.reflection.activation

import kotlin.reflect.KClass

class PriorityTypeActivator(
    private val activators: Collection<TypeActivator>
) : TypeActivator {
    override fun <T : Any> findActivation(type: KClass<T>): TypeActivation<T>? {
        return activators.firstNotNullOfOrNull {
            it.findActivation(type)
        }
    }
}