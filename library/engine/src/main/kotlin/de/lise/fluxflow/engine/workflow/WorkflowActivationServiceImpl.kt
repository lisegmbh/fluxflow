package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.ModelListenerDefinition
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.stereotyped.workflow.ModelListenerDefinitionBuilder

class WorkflowActivationServiceImpl(
    private val modelListenerDefinitionBuilder: ModelListenerDefinitionBuilder
) : WorkflowActivationService {
    @Suppress("UNCHECKED_CAST")
    override fun <TModel> activate(data: WorkflowData): Workflow<TModel> {
        val model = data.model as? TModel
        val listenerDefinitions = model?.let {
            modelListenerDefinitionBuilder.build(model) as List<ModelListenerDefinition<TModel>>
        } ?: emptyList()

        return WorkflowImpl(
            WorkflowDefinitionImpl(
                listenerDefinitions,
            ),
            WorkflowIdentifier(data.id),
            data.model as TModel
        )
    }

}