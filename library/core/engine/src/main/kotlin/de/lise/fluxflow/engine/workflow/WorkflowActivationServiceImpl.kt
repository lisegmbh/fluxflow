package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.action.WorkflowAction
import de.lise.fluxflow.persistence.workflow.WorkflowData

class WorkflowActivationServiceImpl(
    private val workflowDefinitionBuilder: WorkflowDefinitionBuilder,
) : WorkflowActivationService {
    @Suppress("UNCHECKED_CAST")
    override fun <TModel> activate(data: WorkflowData): Workflow<TModel> {
       return activateTypeSafe(data, data.model as Any) as Workflow<TModel>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TModel : Any> activateTypeSafe(data: WorkflowData, model: TModel): Workflow<TModel> {
        val actions = mutableListOf<WorkflowAction<TModel>>()
        val definition = workflowDefinitionBuilder.build(model)
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
}