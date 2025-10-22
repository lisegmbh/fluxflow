package de.lise.fluxflow.api.workflow.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.lise.fluxflow.api.workflow.WorkflowIdentifier

interface WorkflowQueryable<TModel> {
    val identifier: WorkflowIdentifier
    val model: TModel

    companion object {
        val <TRoot> Expression<TRoot, WorkflowQueryable<*>>.identifier
            get() = this.get(WorkflowQueryable<*>::identifier)
        
        val <TRoot, TModel> Expression<TRoot, WorkflowQueryable<TModel>>.model
            get() = this.get(WorkflowQueryable<TModel>::model)
    }
}