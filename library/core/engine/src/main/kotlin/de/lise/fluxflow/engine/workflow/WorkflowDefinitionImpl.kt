package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.ModelListenerDefinition
import de.lise.fluxflow.api.workflow.WorkflowDefinition
import de.lise.fluxflow.api.workflow.action.WorkflowActionDefinition

class WorkflowDefinitionImpl<TModel>(
    override val updateListeners: List<ModelListenerDefinition<TModel>>,
    override val metadata: Map<String, Any> = emptyMap(),
    override val actions: List<WorkflowActionDefinition<TModel>> = emptyList(),
) : WorkflowDefinition<TModel>