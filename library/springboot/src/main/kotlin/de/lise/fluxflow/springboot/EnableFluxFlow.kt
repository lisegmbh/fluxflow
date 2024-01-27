package de.lise.fluxflow.springboot

import de.lise.fluxflow.springboot.configuration.BasicConfiguration
import org.springframework.context.annotation.Import

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Import(BasicConfiguration::class)
annotation class EnableFluxFlow
