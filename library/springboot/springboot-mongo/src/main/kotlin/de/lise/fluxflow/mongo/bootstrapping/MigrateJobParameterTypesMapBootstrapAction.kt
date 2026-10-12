package de.lise.fluxflow.mongo.bootstrapping

import de.lise.fluxflow.mongo.job.JobDocument
import org.springframework.data.mongodb.core.MongoTemplate

class MigrateJobParameterTypesMapBootstrapAction(mongoTemplate: MongoTemplate) : MongoBootstrapAction(mongoTemplate) {
    override fun setup() {
        LegacyTypeMapMigration(mongoTemplate).migrate(
            JobDocument::class.java, "parameterTypes", "parameterTypeMap", "parameterEntries",
        )
    }
}
