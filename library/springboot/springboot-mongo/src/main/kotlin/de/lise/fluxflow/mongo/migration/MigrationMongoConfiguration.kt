package de.lise.fluxflow.mongo.migration

import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnFluxFlowMongo
open class MigrationMongoConfiguration {
    @Bean
    open fun migrationPersistence(
        migrationRepository: MigrationRepository,
        access: FluxFlowMongoAccess,
    ): MigrationPersistence {
        return MigrationMongoPersistence(migrationRepository, access.template)
    }

    @Bean
    open fun mongoMigrationProvider(access: FluxFlowMongoAccess): MongoMigrationProvider {
        return MongoMigrationProvider(access.template)
    }
}
