package de.lise.fluxflow.mongo

import de.lise.fluxflow.mongo.bootstrapping.BootstrapMongoConfiguration
import de.lise.fluxflow.mongo.continuation.history.ContinuationMongoConfiguration
import de.lise.fluxflow.mongo.flowquery.expression.compilation.MongoCompiler
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProvider
import de.lise.fluxflow.mongo.flowquery.expression.compilation.SubclassProviderImpl
import de.lise.fluxflow.mongo.flowquery.repository.MongoQueryTranslator
import de.lise.fluxflow.mongo.job.JobMongoConfiguration
import de.lise.fluxflow.mongo.migration.MigrationMongoConfiguration
import de.lise.fluxflow.mongo.step.StepMongoConfiguration
import de.lise.fluxflow.mongo.step.definition.StepDefinitionMongoConfiguration
import de.lise.fluxflow.mongo.workflow.WorkflowMongoConfiguration
import org.springframework.beans.factory.BeanFactory
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@ConditionalOnFluxFlowMongo
@EnableMongoRepositories
@Import(
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
    open fun subclassProvider(factory: BeanFactory): SubclassProvider {
        return SubclassProviderImpl(AutoConfigurationPackages.get(factory).toSet())
    }

    @Bean
    internal open fun mongoQueryCompiler(subclassProvider: SubclassProvider): MongoCompiler {
        return MongoCompiler(
            subclassProvider,
        )
    }

    @Bean
    internal open fun mongoQueryTranslator(mongoCompiler: MongoCompiler): MongoQueryTranslator {
        return MongoQueryTranslator(mongoCompiler)
    }
}
