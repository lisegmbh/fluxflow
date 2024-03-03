package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.workflow.WorkflowData

interface WorkflowActivationService {
    fun <TModel> activate(data: WorkflowData): Workflow<TModel>
}

