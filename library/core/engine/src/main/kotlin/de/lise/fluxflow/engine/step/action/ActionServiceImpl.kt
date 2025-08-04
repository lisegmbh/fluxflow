package de.lise.fluxflow.engine.step.action

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.step.InvalidStepStateException
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.step.stateful.action.ActionNotFoundException
import de.lise.fluxflow.api.step.stateful.action.ActionService
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.event.action.ActionEvent
import de.lise.fluxflow.engine.step.validation.ValidationService

class ActionServiceImpl(
    private val workflowUpdateService: WorkflowUpdateService,
    private val continuationService: ContinuationService,
    private val validationService: ValidationService,
    private val eventService: EventService,
) : ActionService {
    override fun getActions(step: Step): List<Action> {
        return (step as? StatefulStep)
            ?.actions
            ?: emptyList()
    }

    override fun getActionOrNull(step: Step, kind: ActionKind): Action? {
        return getActions(step)
            .firstOrNull { it.definition.kind == kind }
    }

    override fun getAction(step: Step, kind: ActionKind): Action {
        return getActionOrNull(step, kind)
            ?: throw ActionNotFoundException(
                step,
                kind
            )
    }

    override fun invokeAction(action: Action) {
        validationService.validateBeforeAction(action)
        when (action.step.status) {
            Status.Canceled -> throw InvalidStepStateException("Unable to invoke action on canceled step '${action.step.identifier}'")
            Status.Completed -> throw InvalidStepStateException("Unable to invoke action on completed step '${action.step.identifier}'")
            Status.Active -> {}
        }

        val continuation = action.execute()
        validationService.validateBeforeStepCompletion(action, continuation)


        eventService.publish(ActionEvent(action))
        /**
         *  IMPORTANT: Save changes to workflow before step completion because the step completion would otherwise fetch
         *  the outdated workflow from the database and overwrites the changes.
         */
        @Suppress("UNCHECKED_CAST")
        workflowUpdateService.saveChanges(action.step.workflow as Workflow<Any>)
        continuationService.execute(
            action.step.workflow,
            ReferredWorkflowObject.Companion.create(action.step), 
            continuation,
        )
    }
}