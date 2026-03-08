package de.lise.fluxflow.rest.workflow

import de.lise.fluxflow.rest.ResourceMetadataDto

interface WorkflowMetadataDto : ResourceMetadataDto {
    val workflowIdentifier: String
    val annotations: Map<String, Any>
}