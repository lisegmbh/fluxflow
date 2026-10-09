package de.lise.fluxflow.mongo

import org.springframework.data.mongodb.core.MongoTemplate

/**
 * Applies application-specific settings to FluxFlow's internal [MongoTemplate].
 *
 * The internal template deliberately is not exposed as a Spring bean. Applications that need
 * settings beyond the host template's read preference can provide ordered customizer beans.
 */
fun interface FluxFlowMongoTemplateCustomizer {
    fun customize(mongoTemplate: MongoTemplate)
}
