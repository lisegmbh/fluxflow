package de.lise.fluxflow.reflection.types

/**
 * Indicates that a trusted type manifest or registration is invalid.
 */
class TypeManifestException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
