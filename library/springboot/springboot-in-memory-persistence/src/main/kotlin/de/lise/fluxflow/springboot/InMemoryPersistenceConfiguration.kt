package de.lise.fluxflow.springboot

import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.test.persistence.TestIdGenerator
import de.lise.fluxflow.test.persistence.continuation.history.ContinuationRecordTestPersistence
import de.lise.fluxflow.test.persistence.job.JobTestPersistence
import de.lise.fluxflow.test.persistence.step.StepTestPersistence
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
}