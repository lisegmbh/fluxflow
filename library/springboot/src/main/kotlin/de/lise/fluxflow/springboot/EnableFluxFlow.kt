package de.lise.fluxflow.springboot

import org.springframework.context.annotation.Import

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Import(FluxFlowConfiguration::class)
annotation class EnableFluxFlow()
