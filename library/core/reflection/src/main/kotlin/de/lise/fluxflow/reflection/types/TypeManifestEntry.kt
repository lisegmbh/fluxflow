package de.lise.fluxflow.reflection.types

/**
 * A type declaration read from a trusted build artifact or application configuration.
 */
data class TypeManifestEntry(
    val role: TypeRole,
    val key: String,
    val binaryClassName: String,
    val origin: String,
)
