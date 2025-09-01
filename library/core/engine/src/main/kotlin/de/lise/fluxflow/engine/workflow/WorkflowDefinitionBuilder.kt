package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.WorkflowDefinition
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.workflow.ModelListenerDefinitionBuilder
import de.lise.fluxflow.stereotyped.workflow.action.WorkflowActionDefinitionBuilder

class WorkflowDefinitionBuilder(
    private val modelListenerDefinitionBuilder: ModelListenerDefinitionBuilder,
    private val metadataBuilder: MetadataBuilder,
    private val actionDefinitionBuilder: WorkflowActionDefinitionBuilder
) {
    fun <TModel : Any> build(model: TModel): WorkflowDefinition<TModel> {
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