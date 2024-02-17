package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.workflow.WorkflowData

class WorkflowActivationServiceImpl : WorkflowActivationService {
    @Suppress("UNCHECKED_CAST")
    override fun <TModel> activate(data: WorkflowData): Workflow<TModel> {
        return WorkflowImpl(
            WorkflowDefinitionImpl(
                emptyList() // TODO: Implement
            ),
            WorkflowIdentifier(data.id),
            data.model as TModel
        )
    }
}