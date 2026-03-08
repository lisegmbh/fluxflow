package de.lise.fluxflow.springboot.rest.workflow

import de.lise.fluxflow.api.ExperimentalApi
import de.lise.fluxflow.rest.workflow.WorkflowDto
import de.lise.fluxflow.rest.workflow.WorkflowMetadataDto
import de.lise.fluxflow.rest.workflow.WorkflowSpecDto

@ExperimentalApi
data class DefaultWorkflowDto(
   override val metadata: WorkflowMetadataDto,
   override val spec: WorkflowSpecDto
) : WorkflowDto