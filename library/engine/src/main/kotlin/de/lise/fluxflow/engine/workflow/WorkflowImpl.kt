package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowDefinition
import de.lise.fluxflow.api.workflow.WorkflowIdentifier

class WorkflowImpl<TModel>(
    override val definition: WorkflowDefinition<TModel>,
    override val identifier: WorkflowIdentifier,
    override val model: TModel,
) : Workflow<TModel>