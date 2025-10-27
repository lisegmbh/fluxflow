package de.lise.fluxflow.mongo.bootstrapping

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfiguration
import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfigurer
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.step.StepRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter

@Configuration
@ConditionalOnFluxFlowMongo
@EnableConfigurationProperties(CollationConfiguration::class)
open class BootstrapMongoConfiguration {
    @Bean
    @Order(99)
    open fun createCollectionsWithCollationBootstrapper(
        mongoTemplate: MongoTemplate,
        collationConfigurer: CollationConfigurer
    ): BootstrapAction {
        return CreateCollectionsBootstrapAction(mongoTemplate, collationConfigurer)
    }

    @Bean
    @Order(100)
    open fun indexBootstrapper(mongoTemplate: MongoTemplate): BootstrapAction {
        return CreateIndexesBootstrapAction(mongoTemplate)
    }

    @Bean
    @Order(101)
    open fun dataTypeMapBootstrapper(mongoTemplate: MongoTemplate): BootstrapAction {
        return MigrateDataTypesMapBootstrapAction(mongoTemplate)
    }

    @Bean
    @Order(102)
    open fun metadataTypeMapBootstrapper(mongoTemplate: MongoTemplate): BootstrapAction {
        return MigrateMetadataTypesMapBootstrapAction(mongoTemplate)
    }

    @Bean
    @Order(103)
    open fun parameterTypeMapBootstrapper(mongoTemplate: MongoTemplate): BootstrapAction {
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
            failureAction, stepRepository, jobRepository, mongoConverter, mongoTemplate
        )
    }
}