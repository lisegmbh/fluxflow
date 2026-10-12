package de.lise.fluxflow.mongo

import org.springframework.data.mongodb.core.MongoTemplate

/**
 * Applies application-specific settings to FluxFlow's internal [MongoTemplate].
 *
 * The internal template deliberately is not exposed as a Spring bean. Applications that need
 * settings beyond the host template's read preference can provide ordered customizer beans.
 * Host-template write concern, write concern resolver, write result checking, session
 * synchronization and lifecycle-event settings are not copied automatically. Apply the same
 * configuration to both templates when they should share these settings. Client/database-factory
 * defaults remain effective. Disabling lifecycle events does not disable the type guard.
 */
fun interface FluxFlowMongoTemplateCustomizer {
    fun customize(mongoTemplate: MongoTemplate)
}
