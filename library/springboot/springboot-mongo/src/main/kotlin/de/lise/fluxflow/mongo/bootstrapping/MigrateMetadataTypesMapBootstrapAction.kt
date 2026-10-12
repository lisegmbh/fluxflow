package de.lise.fluxflow.mongo.bootstrapping

import de.lise.fluxflow.mongo.step.StepDocument
import org.springframework.data.mongodb.core.MongoTemplate

class MigrateMetadataTypesMapBootstrapAction(mongoTemplate: MongoTemplate) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        LegacyTypeMapMigration(mongoTemplate).migrate(
            StepDocument::class.java, "metadataTypes", "metadataTypeMap", "metadataEntries",
        )
    }
}
