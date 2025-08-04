package de.lise.fluxflow.engine.workflow.action

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.api.workflow.action.WorkflowAction
import de.lise.fluxflow.api.workflow.action.WorkflowActionNotFoundException
import de.lise.fluxflow.api.workflow.action.WorkflowActionService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.event.action.WorkflowActionEvent

class WorkflowActionServiceImpl(
    private val eventService: EventService,
    private val workflowUpdateService: WorkflowUpdateService,
    private val continuationService: ContinuationService
) : WorkflowActionService {
    override fun <TModel : Any> getActions(workflow: Workflow<TModel>): List<WorkflowAction<TModel>> {
        return workflow.actions
    }

    override fun <TModel : Any> getAction(
        workflow: Workflow<TModel>,
        kind: ActionKind,
    ): WorkflowAction<TModel> {
        return getActions(workflow).firstOrNull { 
            it.definition.kind == kind
        } ?: throw WorkflowActionNotFoundException(
            workflow,
            kind
        )
    }

    override fun <TModel : Any> invokeAction(action: WorkflowAction<TModel>) {
        val continuation = action.execute()
        
        eventService.publish(WorkflowActionEvent(action))
        /**
         *  IMPORTANT: Save changes to workflow before executing the continuation because it would otherwise fetch
         *  the outdated workflow from the database and overwrites the previous changes.
         */
        workflowUpdateService.saveChanges(action.workflow)
        continuationService.execute(
            action.workflow,
            null,
            continuation
        )   
    }
}