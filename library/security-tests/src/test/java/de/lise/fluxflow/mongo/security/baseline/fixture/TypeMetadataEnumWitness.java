package de.lise.fluxflow.mongo.security.baseline.fixture;

import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader;

/** Harmless enum proving that persisted JVM names cannot trigger enum initialization. */
public enum TypeMetadataEnumWitness {
    SAFE;

    static {
        ((WitnessClassLoader) TypeMetadataEnumWitness.class.getClassLoader()).record("enum initialized");
    }
}
