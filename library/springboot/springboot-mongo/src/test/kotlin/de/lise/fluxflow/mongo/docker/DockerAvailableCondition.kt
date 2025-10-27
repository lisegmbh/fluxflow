package de.lise.fluxflow.mongo.docker

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata
import org.testcontainers.DockerClientFactory

class DockerAvailableCondition : Condition, ExecutionCondition {
    override fun matches(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): Boolean {
        return DockerClientFactory.instance().isDockerAvailable
    }

    override fun evaluateExecutionCondition(context: ExtensionContext?): ConditionEvaluationResult {
        try {
            if (DockerClientFactory.instance().isDockerAvailable) {
                return ConditionEvaluationResult.enabled("Docker is available.")
            }
        } catch (_: Throwable) {
        }
        return ConditionEvaluationResult.disabled("Docker is unavailable.") 
    }
}