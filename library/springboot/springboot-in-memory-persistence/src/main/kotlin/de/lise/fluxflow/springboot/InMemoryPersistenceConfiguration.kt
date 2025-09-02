package de.lise.fluxflow.springboot

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
    open fun workflowPersistence(
        idGenerator: TestIdGenerator
    ): WorkflowPersistence {
        return WorkflowTestPersistence(idGenerator)
    }

    @Bean
    open fun stepPersistence(
        idGenerator: TestIdGenerator
    ): StepPersistence {
        return StepTestPersistence(idGenerator)
    }

    @Bean
    open fun jobPersistence(
        idGenerator: TestIdGenerator
    ): JobPersistence {
        return JobTestPersistence(idGenerator)
    }

    @Bean
    open fun continuationRecordPersistence(
        idGenerator: TestIdGenerator
    ): ContinuationRecordPersistence {
        return ContinuationRecordTestPersistence(idGenerator)
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