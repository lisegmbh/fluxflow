package de.lise.fluxflow.reflection.types

import kotlin.reflect.KClass

/**
 * An explicit, compiler-checked registration for a type that is not discovered by annotations.
 */
data class TypeRegistration(
    val role: TypeRole,
    val key: String,
    val type: KClass<*>,
)
