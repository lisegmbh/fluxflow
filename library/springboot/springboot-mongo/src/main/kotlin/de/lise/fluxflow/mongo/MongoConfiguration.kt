package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.bootstrapping.BootstrapMongoConfiguration
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordRepository
import de.lise.fluxflow.mongo.continuation.history.ContinuationMongoConfiguration
import de.lise.fluxflow.mongo.continuation.history.query.QueryableContinuationRecordRepositoryImpl
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompilerConfig
import de.lise.fluxflow.mongo.flowquery.expression.compilation.RegistrySubclassProvider
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProvider
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.job.JobRepository
import de.lise.fluxflow.mongo.job.JobMongoConfiguration
import de.lise.fluxflow.mongo.job.query.QueryableJobRepositoryImpl
import de.lise.fluxflow.mongo.migration.MigrationRepository
import de.lise.fluxflow.mongo.migration.MigrationMongoConfiguration
import de.lise.fluxflow.mongo.step.StepRepository
import de.lise.fluxflow.mongo.step.StepMongoConfiguration
import de.lise.fluxflow.mongo.step.definition.StepDefinitionRepository
import de.lise.fluxflow.mongo.step.definition.StepDefinitionMongoConfiguration
import de.lise.fluxflow.mongo.step.query.QueryableStepRepositoryImpl
import de.lise.fluxflow.mongo.workflow.WorkflowRepository
import de.lise.fluxflow.mongo.workflow.WorkflowMongoConfiguration
import de.lise.fluxflow.mongo.workflow.query.QueryableWorkflowRepositoryImpl
import de.lise.fluxflow.reflection.types.TypeRegistry
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@ConditionalOnFluxFlowMongo
@Import(
    VersionSpecificMongoTypeMapperConfiguration::class,
    WorkflowMongoConfiguration::class,
    StepMongoConfiguration::class,
    JobMongoConfiguration::class,
    ContinuationMongoConfiguration::class,
    StepDefinitionMongoConfiguration::class,
    MigrationMongoConfiguration::class,
    BootstrapMongoConfiguration::class
)
open class MongoConfiguration {
    @Bean
    internal open fun fluxFlowMongoAccess(
        hostTemplate: MongoTemplate,
        registry: TypeRegistry,
        typeMapperFactory: FluxFlowMongoTypeMapperFactory,
        applicationContext: ApplicationContext,
        factory: BeanFactory,
        environment: Environment,
        customizers: ObjectProvider<FluxFlowMongoTemplateCustomizer>,
    ): FluxFlowMongoAccess = FluxFlowMongoAccess(
        hostTemplate,
        registry,
        typeMapperFactory,
        applicationContext,
        factory,
        environment,
        customizers,
    )

    @Bean
    open fun workflowRepository(access: FluxFlowMongoAccess): WorkflowRepository =
        access.repository(
            WorkflowRepository::class.java,
            QueryableWorkflowRepositoryImpl(access.template),
        )

    @Bean
    open fun stepRepository(access: FluxFlowMongoAccess): StepRepository =
        access.repository(
            StepRepository::class.java,
            QueryableStepRepositoryImpl(access.template),
        )

    @Bean
    open fun jobRepository(access: FluxFlowMongoAccess): JobRepository =
        access.repository(
            JobRepository::class.java,
            QueryableJobRepositoryImpl(access.template),
        )

    @Bean
    open fun continuationRecordRepository(access: FluxFlowMongoAccess): ContinuationRecordRepository =
        access.repository(
            ContinuationRecordRepository::class.java,
            QueryableContinuationRecordRepositoryImpl(access.template),
        )

    @Bean
    open fun stepDefinitionRepository(access: FluxFlowMongoAccess): StepDefinitionRepository =
        access.repository(StepDefinitionRepository::class.java)

    @Bean
    open fun migrationRepository(access: FluxFlowMongoAccess): MigrationRepository =
        access.repository(MigrationRepository::class.java)

    @Bean
    open fun subclassProvider(registry: TypeRegistry): SubclassProvider =
        RegistrySubclassProvider(registry)

    @Bean
    internal open fun mongoQueryCompiler(
        subclassProvider: SubclassProvider,
        access: FluxFlowMongoAccess,
    ): MongoCompiler {
        return MongoCompiler(
            subclassProvider,
            config = MongoCompilerConfig(typeFieldName = access.typeKey),
        )
    }

    @Bean
    internal open fun mongoQueryTranslator(mongoCompiler: MongoCompiler): MongoQueryTranslator {
        return MongoQueryTranslator(mongoCompiler)
    }
}
