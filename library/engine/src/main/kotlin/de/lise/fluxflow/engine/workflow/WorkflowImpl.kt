package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.workflow.WorkflowData

class WorkflowImpl<TModel>(
    override val identifier: WorkflowIdentifier,
    override val model: TModel
) : Workflow<TModel> {

    @Suppress("UNCHECKED_CAST")
    internal constructor(
        data: WorkflowData
    ): this(
        WorkflowIdentifier(data.id),
        data.model as TModel
    )
}