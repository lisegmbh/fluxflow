package de.lise.fluxflow.engine.job;

import java.util.concurrent.atomic.AtomicInteger;

/** The method signature is deliberately unavailable to the isolated job loader. */
public final class BuilderLinkageFixture {
    public static final AtomicInteger constructions = new AtomicInteger();

    public static final class MissingMethodParameter {
    }
}
