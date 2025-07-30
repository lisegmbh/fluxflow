package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.ModelListenerDefinition
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.action.WorkflowAction
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.workflow.ModelListenerDefinitionBuilder

class WorkflowActivationServiceImpl(
    private val modelListenerDefinitionBuilder: ModelListenerDefinitionBuilder,
    private val metadataBuilder: MetadataBuilder
) : WorkflowActivationService {
    @Suppress("UNCHECKED_CAST")
    override fun <TModel> activate(data: WorkflowData): Workflow<TModel> {
        val model = data.model as? TModel
        val listenerDefinitions = model?.let {
            modelListenerDefinitionBuilder.build(model) as List<ModelListenerDefinition<TModel>>
        } ?: emptyList()
        
        val metadata = model?.let {
            metadataBuilder.build(it::class)
        } ?: emptyMap()
        
        val definition = WorkflowDefinitionImpl(
            listenerDefinitions,
            metadata
        ) 
        val actions = mutableListOf<WorkflowAction<TModel>>()
        
        val workflow = WorkflowImpl(
            definition,
            WorkflowIdentifier(data.id),
            data.model as TModel,
            metadata,
            actions
        )
        
        actions.addAll(
            definition.actions.map { actionDefinition ->
                actionDefinition.createAction(workflow)
            }   
        )
        
        return workflow
    }
}