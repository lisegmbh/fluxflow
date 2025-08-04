package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowDefinition
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.action.WorkflowAction

class WorkflowImpl<TModel>(
    override val definition: WorkflowDefinition<TModel>,
    override val identifier: WorkflowIdentifier,
    override val model: TModel,
    override val metadata: Map<String, Any>,
    override val actions: List<WorkflowAction<TModel>> = emptyList()
) : Workflow<TModel>