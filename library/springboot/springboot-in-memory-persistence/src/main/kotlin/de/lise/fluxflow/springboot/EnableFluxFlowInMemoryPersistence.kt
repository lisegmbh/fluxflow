package de.lise.fluxflow.springboot

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(InMemoryPersistenceConfiguration::class)
annotation class EnableFluxFlowInMemoryPersistence
