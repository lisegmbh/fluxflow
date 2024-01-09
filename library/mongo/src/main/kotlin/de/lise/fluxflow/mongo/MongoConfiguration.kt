package de.lise.fluxflow.mongo

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.CreateIndexesBootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.MigrateDataTypesMapBootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.MigrateJobParameterTypesMapBootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.MigrateMetadataTypesMapBootstrapAction
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordMongoPersistence
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordRepository
import de.lise.fluxflow.mongo.job.JobMongoPersistence
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.step.StepMongoPersistence
import de.lise.fluxflow.mongo.step.StepRepository
import de.lise.fluxflow.mongo.workflow.WorkflowMongoPersistence
import de.lise.fluxflow.mongo.workflow.WorkflowRepository
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories


@Configuration
@EnableMongoRepositories
@ConditionalOnFluxFlowMongo
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
    open fun continuationRecordPersistence(
        continuationRecordRepository: ContinuationRecordRepository
    ): ContinuationRecordPersistence {
        return ContinuationRecordMongoPersistence(
            continuationRecordRepository
        )
    }

    @Bean
    open fun indexBootstrapper(
        mongoTemplate: MongoTemplate
    ): BootstrapAction {
        return CreateIndexesBootstrapAction(mongoTemplate)
    }

    @Bean
    open fun dataTypeMapBootstrapper(
        mongoTemplate: MongoTemplate
    ): BootstrapAction {
        return MigrateDataTypesMapBootstrapAction(mongoTemplate)
    }
    
    @Bean
    open fun metadataTypeMapBootstrapper(
        mongoTemplate: MongoTemplate
    ): BootstrapAction {
        return MigrateMetadataTypesMapBootstrapAction(mongoTemplate)
    }
    
    @Bean
    open fun parameterTypeMapBootstrapper(
        mongoTemplate: MongoTemplate
    ): BootstrapAction {
        return MigrateJobParameterTypesMapBootstrapAction(mongoTemplate)
    }
}