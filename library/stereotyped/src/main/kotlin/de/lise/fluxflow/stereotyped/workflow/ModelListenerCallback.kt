package de.lise.fluxflow.stereotyped.workflow

import de.lise.fluxflow.api.workflow.Workflow

fun interface ModelListenerCallback<TInstance, TModel> {
    fun call(
        workflow: Workflow<TModel>,
        instance: TInstance,
        oldModel: TModel,
        newModel: TModel
    )
}