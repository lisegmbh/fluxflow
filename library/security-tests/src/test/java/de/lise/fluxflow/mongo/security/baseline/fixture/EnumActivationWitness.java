package de.lise.fluxflow.mongo.security.baseline.fixture;

import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader;

/** Records enum initialization without performing any external action. */
public enum EnumActivationWitness {
    SAFE;

    static {
        ((WitnessClassLoader) EnumActivationWitness.class.getClassLoader())
            .record("enum-initialized");
    }
}
