package de.lise.fluxflow.mongo.generic

/**
 * Indicates that persisted value type metadata is structurally invalid or cannot be applied.
 */
class ValueTypeConversionException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
