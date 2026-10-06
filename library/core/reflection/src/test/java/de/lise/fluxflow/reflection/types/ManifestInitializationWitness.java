package de.lise.fluxflow.reflection.types;

public final class ManifestInitializationWitness {
    static {
        System.setProperty(
            "de.lise.fluxflow.reflection.types.ManifestInitializationWitness.initialized",
            "true"
        );
    }

    private ManifestInitializationWitness() {
    }
}
