package de.lise.fluxflow.mongo.bootstrapping

import de.lise.fluxflow.mongo.step.StepDocument
import org.springframework.data.mongodb.core.MongoTemplate

class MigrateDataTypesMapBootstrapAction(mongoTemplate: MongoTemplate) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        LegacyTypeMapMigration(mongoTemplate).migrate(
            StepDocument::class.java, "dataTypes", "dataTypeMap", "dataEntries",
        )
    }
}
