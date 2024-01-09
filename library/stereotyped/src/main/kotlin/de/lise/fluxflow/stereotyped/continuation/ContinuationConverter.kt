package de.lise.fluxflow.stereotyped.continuation

import de.lise.fluxflow.api.continuation.Continuation

/**
 * A [ContinuationConverter] can be used to convert a function result into a continuation.
 * @param T The type of values supported by this converter.
 */
fun interface ContinuationConverter<T> {
    /**
     * Converts the given value into a continuation.
     * @param value The value to be converted into a conversion.
     */
    fun toContinuation(value: T): Continuation<*>
}