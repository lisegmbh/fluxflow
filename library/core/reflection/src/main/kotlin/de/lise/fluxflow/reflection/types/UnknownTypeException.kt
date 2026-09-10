package de.lise.fluxflow.reflection.types

/**
 * Indicates that no exact registration exists for a type identifier and role.
 */
class UnknownTypeException(
    val role: TypeRole,
    val key: String,
) : IllegalArgumentException(
    "No trusted ${role.manifestName} type is registered for key '$key'."
)
