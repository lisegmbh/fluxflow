package de.lise.fluxflow.mongo.docker

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata
import org.testcontainers.DockerClientFactory

class DockerAvailableCondition : ExecutionCondition, Condition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        return if (DockerClientFactory.instance().isDockerAvailable) {
            ConditionEvaluationResult.enabled("Docker is available")
        } else {
            ConditionEvaluationResult.disabled("Docker is not available")
        }
    }

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        return DockerClientFactory.instance().isDockerAvailable
    }
}
