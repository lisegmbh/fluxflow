package de.lise.fluxflow.springboot.configuration

import de.lise.fluxflow.api.state.ChangeDetector
import de.lise.fluxflow.engine.state.AssumingChangeDetector
import de.lise.fluxflow.engine.state.DefaultChangeDetector
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.workflow.WorkflowData
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ChangeDetectionConfiguration {
    @Bean
    @ConditionalOnProperty(
        name = ["fluxflow.change-detection.workflow"],
        havingValue = "true",
        matchIfMissing = true
    )
    open fun workflowDataChangeDetector(
    ): ChangeDetector<WorkflowData> {
        return DefaultChangeDetector()
    }

    @Bean
    @ConditionalOnProperty(
        name = ["fluxflow.change-detection.workflow"],
        havingValue = "false",
        matchIfMissing = false
    )
    open fun disabledWorkflowDataChangeDetector(
    ): ChangeDetector<WorkflowData> {
        return AssumingChangeDetector(true)
    }

    @Bean
    @ConditionalOnProperty(
        name = ["fluxflow.change-detection.step"],
        havingValue = "true",
        matchIfMissing = true
    )
    open fun stepDataChangeDetector(
    ): ChangeDetector<StepData> {
        return DefaultChangeDetector()
    }

    @Bean
    @ConditionalOnProperty(
        name = ["fluxflow.change-detection.step"],
        havingValue = "false",
        matchIfMissing = false
    )
    open fun disabledStepDataChangeDetector(
    ): ChangeDetector<StepData> {
        return AssumingChangeDetector(true)
    }
}