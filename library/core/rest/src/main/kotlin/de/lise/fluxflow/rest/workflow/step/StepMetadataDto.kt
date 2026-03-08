package de.lise.fluxflow.rest.workflow.step

import de.lise.fluxflow.rest.ResourceMetadataDto

interface StepMetadataDto  : ResourceMetadataDto {
    val workflowIdentifier: String
    val stepIdentifier: String
    val kind: String
    val annotations: Map<String, Any>
}