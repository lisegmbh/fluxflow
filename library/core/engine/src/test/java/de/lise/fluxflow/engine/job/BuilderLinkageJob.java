package de.lise.fluxflow.engine.job;

/** Construction works before the definition builder inspects the missing method type. */
public final class BuilderLinkageJob {
    public BuilderLinkageJob() {
        BuilderLinkageFixture.constructions.incrementAndGet();
    }

    public void execute(BuilderLinkageFixture.MissingMethodParameter ignored) {
    }
}
