package de.lise.fluxflow.reflection.types

/** Identifies the role and key of conflicting trusted registrations. */
class TypeRegistrationConflictException(
    val role: TypeRole,
    val key: String,
    message: String,
) : TypeManifestException(message)
