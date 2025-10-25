package de.lise.fluxflow.mongo

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.mapper.query.QueryMapperImpl
import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.mongo.bootstrapping.*
import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfiguration
import de.lise.fluxflow.mongo.bootstrapping.collation.CollationConfigurer
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordMongoPersistence
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordRepository
import de.lise.fluxflow.mongo.continuation.history.flowquery.ContinuationRecordDataToDocumentMaper
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProvider
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProviderImpl
import de.lise.fluxflow.mongo.flowquery.repository.MongoExecutor
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.mongo.job.JobMongoPersistence
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.job.flowquery.JobDataToDocumentMapper
import de.lise.fluxflow.mongo.migration.MigrationMongoPersistence
import de.lise.fluxflow.mongo.migration.MigrationRepository
import de.lise.fluxflow.mongo.migration.MongoMigrationProvider
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.mongo.step.StepMongoPersistence
import de.lise.fluxflow.mongo.step.StepRepository
import de.lise.fluxflow.mongo.step.definition.StepDefinitionMongoPersistence
import de.lise.fluxflow.mongo.step.definition.StepDefinitionRepository
import de.lise.fluxflow.mongo.step.flowquery.StepDataToDocumentMapper
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.mongo.workflow.WorkflowMongoPersistence
import de.lise.fluxflow.mongo.workflow.WorkflowRepository
import de.lise.fluxflow.mongo.workflow.flowquery.WorkflowDataToDocumentMapper
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
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

    // --------------------------------------------------------------------------------------------
    // Expression Compilation Infrastructure
    // --------------------------------------------------------------------------------------------

    @Bean
    open fun subclassProvider(factory: BeanFactory): SubclassProvider {
        return SubclassProviderImpl(AutoConfigurationPackages.get(factory).toSet())
    }

    @Bean
    internal open fun mongoQueryCompiler(subclassProvider: SubclassProvider): MongoCompiler {
        return MongoCompiler(subclassProvider)
    }

    @Bean
    internal open fun mongoQueryTranslator(mongoCompiler: MongoCompiler): MongoQueryTranslator {
        return MongoQueryTranslator(mongoCompiler)
    }

    // --------------------------------------------------------------------------------------------
    // Executors (type-specific)
    // --------------------------------------------------------------------------------------------

    @Bean
    internal open fun workflowMongoExecutor(mongoTemplate: MongoTemplate): MongoExecutor<WorkflowDocument> {
        return MongoExecutor(mongoTemplate, WorkflowDocument::class.java)
    }

    @Bean
    internal open fun stepMongoExecutor(mongoTemplate: MongoTemplate): MongoExecutor<StepDocument> {
        return MongoExecutor(mongoTemplate, StepDocument::class.java)
    }

    @Bean
    internal open fun jobMongoExecutor(mongoTemplate: MongoTemplate): MongoExecutor<JobDocument> {
        return MongoExecutor(mongoTemplate, JobDocument::class.java)
    }

    @Bean
    internal open fun continuationMongoExecutor(mongoTemplate: MongoTemplate): MongoExecutor<ContinuationRecordDocument> {
        return MongoExecutor(mongoTemplate, ContinuationRecordDocument::class.java)
    }

    // --------------------------------------------------------------------------------------------
    // FlowQuery Repositories
    // --------------------------------------------------------------------------------------------

    @Bean
    internal open fun workflowFlowQueryRepository(
        queryTranslator: MongoQueryTranslator,
        executor: MongoExecutor<WorkflowDocument>,
    ): MongoFlowQueryRepository<WorkflowDocument> {
        return MongoFlowQueryRepository(
            rootType = WorkflowDocument::class,
            translator = queryTranslator,
            executor = executor
        )
    }

    @Bean
    internal open fun stepFlowQueryRepository(
        queryTranslator: MongoQueryTranslator,
        executor: MongoExecutor<StepDocument>,
    ): MongoFlowQueryRepository<StepDocument> {
        return MongoFlowQueryRepository(
            rootType = StepDocument::class,
            translator = queryTranslator,
            executor = executor
        )
    }

    @Bean
    internal open fun jobFlowQueryRepository(
        queryTranslator: MongoQueryTranslator,
        executor: MongoExecutor<JobDocument>,
    ): MongoFlowQueryRepository<JobDocument> {
        return MongoFlowQueryRepository(
            rootType = JobDocument::class,
            translator = queryTranslator,
            executor = executor
        )
    }

    @Bean
    internal open fun continuationFlowQueryRepository(
        queryTranslator: MongoQueryTranslator,
        executor: MongoExecutor<ContinuationRecordDocument>,
    ): MongoFlowQueryRepository<ContinuationRecordDocument> {
        return MongoFlowQueryRepository(
            rootType = ContinuationRecordDocument::class,
            translator = queryTranslator,
            executor = executor
        )
    }

    // --------------------------------------------------------------------------------------------
    // Mappers and Persistence Bindings
    // --------------------------------------------------------------------------------------------

    @Bean
    open fun workflowDocumentMapper(): QueryMapper<WorkflowData, WorkflowDocument> {
        return QueryMapperImpl(WorkflowDataToDocumentMapper())
    }

    @Bean
    open fun workflowPersistence(
        workflowRepository: WorkflowRepository,
        queryableRepository: MongoFlowQueryRepository<WorkflowDocument>,
        queryMapper: QueryMapper<WorkflowData, WorkflowDocument>,
    ): WorkflowPersistence {
        return WorkflowMongoPersistence(workflowRepository, queryableRepository, queryMapper)
    }

    @Bean
    open fun stepDocumentMapper(): QueryMapper<StepData, StepDocument> {
        return QueryMapperImpl(StepDataToDocumentMapper())
    }

    @Bean
    open fun stepPersistence(
        stepRepository: StepRepository,
        queryableRepository: MongoFlowQueryRepository<StepDocument>,
        queryMapper: QueryMapper<StepData, StepDocument>,
    ): StepPersistence {
        return StepMongoPersistence(stepRepository, queryableRepository, queryMapper)
    }

    @Bean
    open fun jobQueryMapper(): QueryMapper<JobData, JobDocument> {
        return QueryMapperImpl(JobDataToDocumentMapper())
    }

    @Bean
    open fun jobPersistence(
        jobRepository: JobRepository,
        queryableRepository: MongoFlowQueryRepository<JobDocument>,
        queryMapper: QueryMapper<JobData, JobDocument>,
    ): JobPersistence {
        return JobMongoPersistence(jobRepository, queryableRepository, queryMapper)
    }

    @Bean
    open fun continuationQueryMapper(): QueryMapper<ContinuationRecordData, ContinuationRecordDocument> {
        return QueryMapperImpl(ContinuationRecordDataToDocumentMaper())
    }

    @Bean
    open fun continuationRecordPersistence(
        continuationRecordRepository: ContinuationRecordRepository,
        queryableRepository: MongoFlowQueryRepository<ContinuationRecordDocument>,
        queryMapper: QueryMapper<ContinuationRecordData, ContinuationRecordDocument>,
    ): ContinuationRecordPersistence {
        return ContinuationRecordMongoPersistence(
            continuationRecordRepository = continuationRecordRepository,
            queryableRepository = queryableRepository,
            queryMapper = queryMapper
        )
    }

    // --------------------------------------------------------------------------------------------
    // Other Mongo Persistences (unchanged)
    // --------------------------------------------------------------------------------------------

    @Bean
    open fun migrationPersistence(
        migrationRepository: MigrationRepository,
        mongoTemplate: MongoTemplate
    ): MigrationPersistence {
        return MigrationMongoPersistence(migrationRepository, mongoTemplate)
    }

    @Bean
    open fun stepDefinitionPersistence(
        stepDefinitionRepository: StepDefinitionRepository,
        mongoTemplate: MongoTemplate
    ): StepDefinitionPersistence {
        return StepDefinitionMongoPersistence(stepDefinitionRepository, mongoTemplate)
    }

    @Bean
    open fun mongoMigrationProvider(mongoTemplate: MongoTemplate): MongoMigrationProvider {
        return MongoMigrationProvider(mongoTemplate)
    }

    // --------------------------------------------------------------------------------------------
    // Bootstrap Actions
    // --------------------------------------------------------------------------------------------

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
            failureAction,
            stepRepository,
            jobRepository,
            mongoConverter,
            mongoTemplate
        )
    }
}