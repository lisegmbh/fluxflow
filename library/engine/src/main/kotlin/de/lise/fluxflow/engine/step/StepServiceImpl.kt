package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.state.ChangeDetector
import de.lise.fluxflow.api.step.*
import de.lise.fluxflow.api.step.query.StepQuery
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.continuation.StepFinalizationSemaphore
import de.lise.fluxflow.engine.event.step.StepCreatedEvent
import de.lise.fluxflow.engine.event.step.StepUpdatedEvent
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.query.toDataQuery
import de.lise.fluxflow.query.pagination.Page
import org.slf4j.LoggerFactory

class StepServiceImpl(
    private val persistence: StepPersistence,
    private val stepActivationService: StepActivationService,
    private val eventService: EventService,
    private val continuationService: ContinuationService,
    private val changeDetector: ChangeDetector<StepData>,
) : StepService {
    fun create(workflow: Workflow<*>, definition: StepDefinition): StepCreationResult {
        val step = definition.createStep(
            workflow,
            StepIdentifier(persistence.randomId()),
            Status.Active
        )

        createData(step)
        eventService.publish(StepCreatedEvent(step))

        val onCreatedAutomations = definition.onCreatedAutomations.map { it.createAutomation(step) }

        return StepCreationResult(
            step,
            onCreatedAutomations
        )
    }

    private fun createData(step: Step): StepData {
        return persistence.create(
            StepData(
                step.identifier.value,
                step.workflow.id.value,
                step.definition.kind.value,
                fetchData(step),
                step.status,
                step.metadata
            ),
        )
    }

    private fun fetchData(step: Step): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>()

        if (step is StatefulStep) {
            step.data
                .associateWith { it.get() }
                .forEach {
                    data[it.key.definition.kind.value] = it.value
                }
        }

        return data
    }

    override fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>): List<Step> {
        return persistence.findForWorkflow(workflow.id)
            .map { stepActivationService.activate(workflow, it) }
    }

    override fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>, query: StepQuery): Page<Step> {
        return persistence.findForWorkflow(
            workflow.id,
            query.toDataQuery()
        ).map { stepActivationService.activate(workflow, it) }
    }

    override fun <TWorkflowModel> findStep(workflow: Workflow<TWorkflowModel>, stepIdentifier: StepIdentifier): Step? {
        return persistence.findForWorkflowAndId(workflow.id, stepIdentifier)
            ?.let { stepActivationService.activate(workflow, it) }
    }

    override fun reactivate(step: Step): Step {
        if (step.status == Status.Active) {
            return step
        }
        return setState(step, Status.Active)
    }

    override fun complete(step: Step): Step {
        if (step.status == Status.Completed) {
            throw InvalidStepStateException("Cannot complete already completed step '${step.identifier}'")
        }
        val originatingObject = ReferredWorkflowObject.create(step)
        step.definition.onCompletedAutomations
            .map { it.createAutomation(step) }
            .map { it.execute() }
            .map {
                continuationService.execute(
                    step.workflow,
                    originatingObject,
                    it,
                    StepFinalizationSemaphore(true)
                )
            }
        return setState(step, Status.Completed)
    }

    override fun cancel(step: Step): Step {
        return setState(step, Status.Canceled)
    }

    private fun setState(step: Step, status: Status): Step {
        val stepData = persistence.findForWorkflowAndId(step.workflow.id, step.identifier)!!
        if(step.status == status) {
            return step
        }
        return saveChanges(step, stepData.withState(status))
    }

    fun saveChanges(step: Step): Step {
        val oldVersion = persistence.findForWorkflowAndId(step.workflow.id, step.identifier)!!
        val newVersion = oldVersion.withData(fetchData(step))

        if(!changeDetector.hasChanged(oldVersion, newVersion)) {
            Logger.debug(
                "Step '{}' of workflow '{}' will not be persisted, because it didn't change.",
                step.identifier.value,
                step.workflow.id.value
            )
            return step
        }

        return saveChanges(step, newVersion)
    }

    private fun saveChanges(step: Step, data: StepData): Step {
        val updatedStep = stepActivationService.activate(step.workflow, persistence.save(data))
        eventService.publish(StepUpdatedEvent(step))

        return updatedStep
    }

    fun deleteAllForWorkflow(workflowIdentifier: WorkflowIdentifier) {
        persistence.findForWorkflow(workflowIdentifier)
            .map { StepIdentifier(it.id) }
            .toSet()
            .let { persistence.deleteMany(it) }
    }

    private companion object {
        val Logger = LoggerFactory.getLogger(StepServiceImpl::class.java)!!
    }
}