package de.lise.fluxflow.mongo.bootstrapping

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.mongo.ConditionalOnFluxFlowMongo
import de.lise.fluxflow.mongo.FluxFlowMongoAccess
import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfiguration
import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfigurer
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.step.StepRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order

@Configuration
@ConditionalOnFluxFlowMongo
@EnableConfigurationProperties(CollationConfiguration::class)
open class BootstrapMongoConfiguration {
    @Bean
    @Order(99)
    open fun createCollectionsWithCollationBootstrapper(
        access: FluxFlowMongoAccess,
        collationConfigurer: CollationConfigurer
    ): BootstrapAction {
        return CreateCollectionsBootstrapAction(access.template, collationConfigurer)
    }

    @Bean
    @Order(100)
    open fun indexBootstrapper(access: FluxFlowMongoAccess): BootstrapAction {
        return CreateIndexesBootstrapAction(access.template)
    }

    @Bean
    @Order(101)
    open fun dataTypeMapBootstrapper(access: FluxFlowMongoAccess): BootstrapAction {
        return MigrateDataTypesMapBootstrapAction(access.template)
    }

    @Bean
    @Order(102)
    open fun metadataTypeMapBootstrapper(access: FluxFlowMongoAccess): BootstrapAction {
        return MigrateMetadataTypesMapBootstrapAction(access.template)
    }

    @Bean
    @Order(103)
    open fun parameterTypeMapBootstrapper(access: FluxFlowMongoAccess): BootstrapAction {
        return MigrateJobParameterTypesMapBootstrapAction(access.template)
    }

    @Bean
    @Order(104)
    open fun migrateToTypeRecordsBootstrapAction(
        @Value("\${fluxflow.mongo.migrations.typeRecords:fail}")
        failureAction: PartialFailureAction,
        stepRepository: StepRepository,
        jobRepository: JobRepository,
        access: FluxFlowMongoAccess,
    ): BootstrapAction {
        return MigrateToTypeRecordsBootstrapAction(
            failureAction,
            stepRepository,
            jobRepository,
            access.converter,
            access.template,
            access.valueTypes,
        )
    }
}
