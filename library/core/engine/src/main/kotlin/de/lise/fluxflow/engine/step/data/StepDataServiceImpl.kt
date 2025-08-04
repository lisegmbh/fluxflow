package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.step.InvalidStepStateException
import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.data.*
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowUpdateService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.event.step.data.StepDataEvent
import de.lise.fluxflow.engine.step.StepServiceImpl

class StepDataServiceImpl(
    private val stepServiceImpl: StepServiceImpl,
    private val workflowUpdateService: WorkflowUpdateService,
    private val eventService: EventService,
    private val globalAllowInactiveModification: Boolean,
    private val continuationService: ContinuationService
) : StepDataService {
    override fun getData(step: Step): List<Data<*>> {
        return (step as? StatefulStep)?.data ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getData(step: Step, kind: DataKind): Data<T>? {
        return getData(step).firstOrNull {
            it.definition.kind == kind
        }?.let {
            it as Data<T>
        }
    }

    override fun <T> setValue(data: Data<T>, value: T) {
        doSetValue(null, data, value)
    }

    override fun <T> setValue(context: Any, data: Data<T>, value: T) {
        doSetValue(context, data, value)
    }

    private fun <T> doSetValue(context: Any?, data: Data<T>, value: T) {
        if (modificationRequiresActiveStep(data)) {
            when (data.step.status) {
                Status.Canceled -> throw InvalidStepStateException("Unable to invoke action on canceled step '${data.step.identifier}'")
                Status.Completed -> throw InvalidStepStateException("Unable to invoke action on completed step '${data.step.identifier}'")
                Status.Active -> {}
            }
        }
        if (data !is ModifiableData) {
            throw UnmodifiableDataException("The data entry '${data.definition.kind}' of step ${data.step.identifier} is not modifiable")
        }

        val oldValue = data.get()
        data.set(value)

        val continuations = data.definition.updateListeners
            .map { it.create(data.step) }
            .map { it.onChange(oldValue, value) }

        if(continuations.isNotEmpty()) {
            continuationService.execute(
                data.step.workflow,
                ReferredWorkflowObject.Companion.create(data.step),
                // Actively setting "Preserve" in order to be independent from default behavior
                Continuation.multiple(continuations).withStatusBehavior(StatusBehavior.Preserve)
            )
        }

        stepServiceImpl.saveChanges(data.step)
        @Suppress("UNCHECKED_CAST")
        workflowUpdateService.saveChanges(data.step.workflow as Workflow<Any>)

        /*
            When publishing the StepDataEvent,
            the workflow should be in a clean state (= all changes should be persisted).
            Listeners should not expect that their changes will be persisted implicitly.
         */
        eventService.publish(StepDataEvent(data, oldValue, value, context))
    }

    private fun modificationRequiresActiveStep(data: Data<*>): Boolean {
        return when  (data.definition.modificationPolicy) {
            ModificationPolicy.AllowInactiveModification -> false
            ModificationPolicy.PreventInactiveModification -> true
            ModificationPolicy.InheritSetting -> !globalAllowInactiveModification
        }
    }
}