package de.lise.fluxflow.springboot.autoconfigure

import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.engine.step.StepActivationService
import de.lise.fluxflow.springboot.cache.memory.step.MemoryCachingStepActivationService
import de.lise.fluxflow.springboot.cache.memory.step.MemoryCachingStepService
import de.lise.fluxflow.springboot.cache.memory.step.StepCache
import de.lise.fluxflow.springboot.cache.memory.workflow.MemoryCachingWorkflowService
import de.lise.fluxflow.springboot.cache.memory.workflow.WorkflowCache
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnProperty(
    value = ["fluxflow.caching.in-memory"],
    havingValue = "true",
    matchIfMissing = true
)
open class MemoryCachingConfiguration {

    @Bean
    open fun workflowCache(): WorkflowCache {
        return WorkflowCache()
    }

    @Bean
    @Primary
    open fun cachedWorkflowService(
        workflowService: WorkflowService,
        workflowCache: WorkflowCache
    ): WorkflowService {
        return MemoryCachingWorkflowService(
            workflowService,
            workflowCache
        )
    }

    @Bean
    open fun stepCache(): StepCache {
        return StepCache()
    }

    @Bean
    @Primary
    open fun cachedStepService(
        stepService: StepService,
        stepCache: StepCache
    ): StepService {
        return MemoryCachingStepService(
            stepService,
            stepCache,
        )
    }

    @Bean
    @Primary
    open fun cachedStepActivationService(
        stepActivationService: StepActivationService,
        stepCache: StepCache
    ): StepActivationService {
        return MemoryCachingStepActivationService(
            stepActivationService,
            stepCache
        )
    }
}