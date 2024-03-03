package de.lise.fluxflow.mongo

import org.springframework.context.annotation.Import

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(MongoConfiguration::class)
annotation class EnableFluxFlowMongoPersistence
