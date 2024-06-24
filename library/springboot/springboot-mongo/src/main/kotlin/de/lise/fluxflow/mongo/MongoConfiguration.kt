package de.lise.fluxflow.mongo

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.*
import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfiguration
import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfigurer
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordMongoPersistence
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordRepository
import de.lise.fluxflow.mongo.job.JobMongoPersistence
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.migration.MigrationMongoPersistence
import de.lise.fluxflow.mongo.migration.MigrationRepository
import de.lise.fluxflow.mongo.migration.MongoMigrationProvider
import de.lise.fluxflow.mongo.step.StepMongoPersistence
import de.lise.fluxflow.mongo.step.StepRepository
import de.lise.fluxflow.mongo.step.definition.StepDefinitionMongoPersistence
import de.lise.fluxflow.mongo.step.definition.StepDefinitionRepository
import de.lise.fluxflow.mongo.workflow.WorkflowMongoPersistence
import de.lise.fluxflow.mongo.workflow.WorkflowRepository
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories


@Configuration
@EnableMongoRepositories
@ConditionalOnFluxFlowMongo
@EnableConfigurationProperties(CollationConfiguration::class)
open class MongoConfiguration {
    @Bean
    open fun workflowPersistence(
        workflowRepository: WorkflowRepository,
    ): WorkflowPersistence {
        return WorkflowMongoPersistence(
            workflowRepository,
        )
    }

    @Bean
    open fun stepPersistence(
        stepRepository: StepRepository,
    ): StepPersistence {
        return StepMongoPersistence(
            stepRepository,
        )
    }

    @Bean
    open fun jobPersistence(
        jobRepository: JobRepository
    ): JobPersistence {
        return JobMongoPersistence(
            jobRepository
        )
    }

    @Bean
    open fun migrationPersistence(
        migrationRepository: MigrationRepository,
        mongoTemplate: MongoTemplate
    ): MigrationPersistence {
        return MigrationMongoPersistence(
            migrationRepository,
            mongoTemplate
        )
    }
    
    @Bean
    open fun continuationRecordPersistence(
        continuationRecordRepository: ContinuationRecordRepository
    ): ContinuationRecordPersistence {
        return ContinuationRecordMongoPersistence(
            continuationRecordRepository
        )
    }

    @Bean
    open fun stepDefinitionPersistence(
        stepDefinitionRepository: StepDefinitionRepository,
        mongoTemplate: MongoTemplate
    ): StepDefinitionPersistence {
        return StepDefinitionMongoPersistence(
            stepDefinitionRepository,
            mongoTemplate
        )
    }
    
    @Bean
    open fun mongoMigrationProvider(
        mongoTemplate: MongoTemplate
    ): MongoMigrationProvider {
        return MongoMigrationProvider(
            mongoTemplate
        )
    }
    
    @Bean
    @Order(99)
    open fun createCollectionsWithCollationBootstrapper(
        mongoTemplate: MongoTemplate,
        collationConfigurer: CollationConfigurer
    ): BootstrapAction {
        return CreateCollectionsBootstrapAction(
            mongoTemplate,
            collationConfigurer
        )
    }

    @Bean
    @Order(100)
    open fun indexBootstrapper(
        mongoTemplate: MongoTemplate
    ): BootstrapAction {
        return CreateIndexesBootstrapAction(mongoTemplate)
    }

    @Bean
    @Order(101)
    open fun dataTypeMapBootstrapper(
        mongoTemplate: MongoTemplate
    ): BootstrapAction {
        return MigrateDataTypesMapBootstrapAction(mongoTemplate)
    }
    
    @Bean
    @Order(102)
    open fun metadataTypeMapBootstrapper(
        mongoTemplate: MongoTemplate
    ): BootstrapAction {
        return MigrateMetadataTypesMapBootstrapAction(mongoTemplate)
    }
    
    @Bean
    @Order(103)
    open fun parameterTypeMapBootstrapper(
        mongoTemplate: MongoTemplate
    ): BootstrapAction {
        return MigrateJobParameterTypesMapBootstrapAction(mongoTemplate)
    }

    @Bean
    @Order(104)
    open fun migrateToTypeRecordsBootstrapAction(
        @Value("\${fluxflow.mongo.migrations.typeRecords:fail}")
        failureAction: PartialFailureAction,
        stepRepository: StepRepository,
        jobRepository: JobRepository,
        mongoTemplate: MongoTemplate,
        mongoConverter: MongoConverter
    ): BootstrapAction {
        return MigrateToTypeRecordsBootstrapAction(
            failureAction,
            stepRepository,
            jobRepository,
            mongoConverter,
            mongoTemplate,
        )
    }
}