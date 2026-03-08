package de.lise.fluxflow.rest.workflow

import de.lise.fluxflow.rest.ResourceDto

interface WorkflowDto : ResourceDto<WorkflowMetadataDto, WorkflowSpecDto> {
    override val kind: String
        get() = "workflow"
}