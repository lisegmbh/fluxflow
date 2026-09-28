package de.lise.fluxflow.springboot.types.fixtures.defaultapp;

import de.lise.fluxflow.stereotyped.step.Step;

@Step
public final class ScanInitializationWitness {
    static {
        System.setProperty(
            "de.lise.fluxflow.springboot.types.fixtures.defaultapp.ScanInitializationWitness.initialized",
            "true"
        );
    }

    private ScanInitializationWitness() {
    }
}
