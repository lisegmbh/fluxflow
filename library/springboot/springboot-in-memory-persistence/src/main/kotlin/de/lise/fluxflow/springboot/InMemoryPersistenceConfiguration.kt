package de.lise.fluxflow.springboot

import de.fluxflow.flowquery.expression.compilation.ClasspathSubclassProvider
import de.fluxflow.flowquery.expression.compilation.StaticSubclassProvider
import de.fluxflow.flowquery.expression.compilation.SubclassProvider
import de.fluxflow.flowquery.inmemory.expression.compilation.InMemoryCompiler
import org.springframework.beans.factory.BeanFactory
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import de.lise.fluxflow.migration.MigrationProvider
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.migration.MigrationPersistence
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.definition.StepDefinitionPersistence
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.test.persistence.TestIdGenerator
import de.lise.fluxflow.test.persistence.continuation.history.ContinuationRecordTestPersistence
import de.lise.fluxflow.test.persistence.job.JobTestPersistence
import de.lise.fluxflow.test.persistence.migration.InMemoryMigrationProvider
import de.lise.fluxflow.test.persistence.migration.MigrationTestPersistence
import de.lise.fluxflow.test.persistence.step.StepTestPersistence
import de.lise.fluxflow.test.persistence.step.definition.StepDefinitionTestPersistence
import de.lise.fluxflow.test.persistence.workflow.WorkflowTestPersistence
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class InMemoryPersistenceConfiguration {
    @Bean
    open fun idGenerator(): TestIdGenerator {
        return TestIdGenerator()
    }

    @Bean
    open fun subclassProvider(factory: BeanFactory): SubclassProvider {
        val basePackages = runCatching { AutoConfigurationPackages.get(factory).toSet() }
            .getOrElse { emptySet() }
        return if (basePackages.isEmpty()) {
            StaticSubclassProvider()
        } else {
            ClasspathSubclassProvider(basePackages)
        }
    }

    @Bean
    open fun inMemoryCompiler(subclassProvider: SubclassProvider): InMemoryCompiler {
        return InMemoryCompiler(subclassProvider)
    }

    @Bean
    open fun workflowPersistence(
        idGenerator: TestIdGenerator,
        inMemoryCompiler: InMemoryCompiler
    ): WorkflowPersistence {
        return WorkflowTestPersistence(
            idGenerator = idGenerator,
            inMemoryCompiler = inMemoryCompiler
        )
    }

    @Bean
    open fun stepPersistence(
        idGenerator: TestIdGenerator,
        inMemoryCompiler: InMemoryCompiler
    ): StepPersistence {
        return StepTestPersistence(
            idGenerator = idGenerator,
            inMemoryCompiler = inMemoryCompiler
        )
    }

    @Bean
    open fun jobPersistence(
        idGenerator: TestIdGenerator,
        inMemoryCompiler: InMemoryCompiler
    ): JobPersistence {
        return JobTestPersistence(
            idGenerator = idGenerator,
            inMemoryCompiler = inMemoryCompiler
        )
    }

    @Bean
    open fun continuationRecordPersistence(
        idGenerator: TestIdGenerator,
        inMemoryCompiler: InMemoryCompiler
    ): ContinuationRecordPersistence {
        return ContinuationRecordTestPersistence(
            idGenerator = idGenerator,
            inMemoryCompiler = inMemoryCompiler    
        )
    }

    @Bean
    open fun migrationPersistence(
        idGenerator: TestIdGenerator
    ): MigrationPersistence {
        return MigrationTestPersistence(idGenerator)
    }

    @Bean
    open fun inMemoryMigrationProvider(): MigrationProvider {
        return InMemoryMigrationProvider()
    }
    
    @Bean
    open fun inMemoryStepDefinitionPersistence(): StepDefinitionPersistence {
        return StepDefinitionTestPersistence()
    }
}