package de.lise.fluxflow.persistence.step

import de.lise.fluxflow.api.step.Status

data class StepData(
    val id: String,
    val workflowId: String,
    val kind: String,
    val version: String?,
    val data: Map<String, Any?>,
    val status: Status,
    val metadata: Map<String, Any>
) {
    fun withData(data: Map<String, Any?>): StepData {
        return StepData(
            id,
            workflowId,
            kind,
            version,
            data,
            status,
            metadata
        )
    }

    fun withState(status: Status): StepData {
        return StepData(
            id,
            workflowId,
            kind,
            version,
            data,
            status,
            metadata
        )
    }
}
