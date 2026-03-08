package de.lise.fluxflow.rest.workflow.step.data

import de.lise.fluxflow.rest.ResourceMetadataDto

interface StepDataMetadataDto : ResourceMetadataDto {
    val workflowIdentifier: String
    val stepIdentifier: String
    val kind: String
    val readonly: Boolean
    val annotations: Map<String, Any>
}