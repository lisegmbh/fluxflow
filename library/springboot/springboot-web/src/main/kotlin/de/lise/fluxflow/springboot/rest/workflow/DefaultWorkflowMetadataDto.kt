package de.lise.fluxflow.springboot.rest.workflow

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.rest.workflow.WorkflowMetadataDto

@ExperimentalApi
data class DefaultWorkflowMetadataDto(
    override val workflowIdentifier: String,
    override val annotations: Map<String, Any>
): WorkflowMetadataDto