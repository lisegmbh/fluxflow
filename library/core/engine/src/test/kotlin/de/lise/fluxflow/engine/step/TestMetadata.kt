package de.lise.fluxflow.engine.step

import de.lise.fluxflow.stereotyped.metadata.Metadata

@Metadata("testMetadata")
@Retention(AnnotationRetention.RUNTIME)
annotation class TestMetadata(val value: String)