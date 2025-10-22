package de.lise.fluxflow.api.workflow.flowquery

import de.lise.fluxflow.api.workflow.WorkflowIdentifier

interface WorkflowQueryable<TModel> {
    val identifier: WorkflowIdentifier
    val model: TModel
}