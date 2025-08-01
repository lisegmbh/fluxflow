package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowDefinition
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.action.WorkflowAction
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.workflow.ModelListenerDefinitionBuilder
import de.lise.fluxflow.stereotyped.workflow.action.WorkflowActionDefinitionBuilder

class WorkflowActivationServiceImpl(
    private val modelListenerDefinitionBuilder: ModelListenerDefinitionBuilder,
    private val metadataBuilder: MetadataBuilder,
    private val actionDefinitionBuilder: WorkflowActionDefinitionBuilder,
) : WorkflowActivationService {
    @Suppress("UNCHECKED_CAST")
    override fun <TModel> activate(data: WorkflowData): Workflow<TModel> {
       return activateTypeSafe(data, data.model as Any) as Workflow<TModel>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TModel : Any> activateTypeSafe(data: WorkflowData, model: TModel): Workflow<TModel> {
        val actions = mutableListOf<WorkflowAction<TModel>>()
        val definition = buildDefinition(model)

        val workflow = WorkflowImpl(
            definition,
            WorkflowIdentifier(data.id),
            data.model as TModel,
            definition.metadata,
            actions
        )

        actions.addAll(
            definition.actions.map { actionDefinition ->
                actionDefinition.createAction(workflow)
            }
        )

        return workflow
    }

    private fun <TModel : Any> buildDefinition(model: TModel): WorkflowDefinition<TModel> {
        val modelListenerDefinitions = modelListenerDefinitionBuilder.build(model)
        val modelType = model::class
        val metadata = metadataBuilder.build(modelType)
        val actionDefinitions = actionDefinitionBuilder.build(modelType)
        
        return WorkflowDefinitionImpl(
            modelListenerDefinitions,
            metadata,
            actionDefinitions
        )
    }
}