package de.lise.fluxflow.mongo.migration

import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@ConditionalOnFluxFlowMongo
open class MigrationMongoConfiguration {
    @Bean
    open fun migrationPersistence(
        migrationRepository: MigrationRepository,
        mongoTemplate: MongoTemplate
    ): MigrationPersistence {
        return MigrationMongoPersistence(migrationRepository, mongoTemplate)
    }

    @Bean
    open fun mongoMigrationProvider(mongoTemplate: MongoTemplate): MongoMigrationProvider {
        return MongoMigrationProvider(mongoTemplate)
    }
}