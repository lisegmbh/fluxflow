package de.lise.fluxflow.stereotyped.workflow.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.Workflow

fun interface WorkflowActionCaller<TModel> {
    fun call(
        workflow: Workflow<TModel>,
    ): Continuation<*>
}