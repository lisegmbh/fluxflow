package de.lise.fluxflow.mongo.security.baseline.fixture;

import de.lise.fluxflow.mongo.security.baseline.WitnessClassLoader;

/** Records class initialization and construction; performs no external action. */
public final class ActivationWitness {
    static {
        ((WitnessClassLoader) ActivationWitness.class.getClassLoader()).record("initialized");
    }

    public ActivationWitness() {
        ((WitnessClassLoader) ActivationWitness.class.getClassLoader()).record("constructed");
    }

    public void execute() {
        // A valid job payload, deliberately never invoked by these activation tests.
    }
}
