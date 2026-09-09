package de.lise.fluxflow.springboot.types;

import de.lise.fluxflow.reflection.types.TypeRegistration;
import de.lise.fluxflow.reflection.types.TypeRegistry;
import de.lise.fluxflow.reflection.types.TypeRole;

/**
 * Compiles against the published API variant to guard the documented consumer surface.
 */
final class TypeRegistryApiConsumer {
    private final TypeRegistry registry;
    private final TypeRegistration registration;
    private final TypeRole role;
    private final TypeRegistrationContributor contributor;

    TypeRegistryApiConsumer(
        TypeRegistry registry,
        TypeRegistration registration,
        TypeRole role,
        TypeRegistrationContributor contributor
    ) {
        this.registry = registry;
        this.registration = registration;
        this.role = role;
        this.contributor = contributor;
    }
}
