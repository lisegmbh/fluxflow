package de.lise.fluxflow.springboot.types

import de.lise.fluxflow.reflection.types.TypeRegistration

/**
 * Contributes explicit type registrations to one application context.
 */
fun interface TypeRegistrationContributor {
    fun registrations(): Iterable<TypeRegistration>
}
