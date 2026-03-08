package de.lise.fluxflow.springboot.rest.workflow

import de.lise.fluxflow.rest.workflow.WorkflowSpecDto

data class DefaultWorkflowSpecDto(
    override val model: Any?
): WorkflowSpecDto