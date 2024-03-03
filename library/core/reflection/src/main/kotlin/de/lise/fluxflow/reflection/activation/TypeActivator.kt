package de.lise.fluxflow.reflection.activation

import kotlin.reflect.KClass

interface TypeActivator {
    fun <T: Any> findActivation(type: KClass<T>): TypeActivation<T>?
}