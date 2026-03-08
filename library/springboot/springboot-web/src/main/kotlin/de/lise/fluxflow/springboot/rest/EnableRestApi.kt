package de.lise.fluxflow.springboot.rest

import de.lise.fluxflow.api.ExperimentalApi
import org.springframework.context.annotation.Import

@ExperimentalApi
@Import(RestConfiguration::class)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableRestApi