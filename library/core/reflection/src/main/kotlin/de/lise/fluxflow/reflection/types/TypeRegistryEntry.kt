package de.lise.fluxflow.reflection.types

import kotlin.reflect.KClass

/** A resolved trusted type and the origins that registered it. */
data class TypeRegistryEntry(
    val role: TypeRole,
    val key: String,
    val binaryClassName: String,
    val type: KClass<*>,
    val origins: List<String>,
)
