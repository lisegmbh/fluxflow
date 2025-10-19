package de.lise.fluxflow.api.workflow.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier


val <TRoot, TModel> Expression<TRoot, Workflow<TModel>>.model: Expression<TRoot, TModel>
    get() {
        return this.get(Workflow<TModel>::model)
    }

val <TRoot, TModel> Expression<TRoot, Workflow<TModel>>.identifier: Expression<TRoot, WorkflowIdentifier>
    get() {
        return this.get(Workflow<TModel>::identifier)
    }