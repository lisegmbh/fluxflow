package de.lise.fluxflow.persistence.step.definition


data class StepDefinitionData(
    val kind: String,
    val version: String,
    val metadata: Map<String, Any>,
    val data: List<DataDefinitionData>
)
