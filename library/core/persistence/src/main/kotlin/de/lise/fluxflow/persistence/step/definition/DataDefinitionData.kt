package de.lise.fluxflow.persistence.step.definition

data class DataDefinitionData(
    val kind: String,
    val type: String,
    val metadata: Map<String, Any>,
)