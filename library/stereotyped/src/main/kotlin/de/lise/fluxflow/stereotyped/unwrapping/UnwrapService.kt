package de.lise.fluxflow.stereotyped.unwrapping

import de.lise.fluxflow.api.step.Step

/**
 * The [UnwrapService] can be used to obtain the original Java/Kotlin object that defines an API object's behavior.
 */
@Deprecated("Experimental feature that might be removed or changed at any time.")
interface UnwrapService {
    /**
     * Returns the original Java/Kotlin object that backs the given step.
     * @throws IllegalUnwrapException if the given step is not backed/defined by a JVM object.
     */
    fun <T: Any> unwrap(step: Step): T
}