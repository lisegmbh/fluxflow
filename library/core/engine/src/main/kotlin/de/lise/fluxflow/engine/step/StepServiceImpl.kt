package de.lise.fluxflow.engine.step

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.state.ChangeDetector
import de.lise.fluxflow.api.step.*
import de.lise.fluxflow.api.step.query.StepQuery
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.versioning.CompatibilityTester
import de.lise.fluxflow.api.versioning.VersionCompatibility
import de.lise.fluxflow.api.versioning.VersionRecorder
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowNotFoundException
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.api.workflow.query.filter.WorkflowFilter
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.continuation.StepFinalizationSemaphore
import de.lise.fluxflow.engine.event.step.StepCreatedEvent
import de.lise.fluxflow.engine.event.step.StepUpdatedEvent
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.step.query.toDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.filter.Filter
import de.lise.fluxflow.query.pagination.Page
import org.slf4j.LoggerFactory

class StepServiceImpl(
    private val persistence: StepPersistence,
    private val stepActivationService: StepActivationService,
    private val eventService: EventService,
    private val continuationService: ContinuationService,
    private val changeDetector: ChangeDetector<StepData>,
    private val stepDefinitionVersionRecorder: VersionRecorder<StepDefinition>,
    private val workflowQueryService: WorkflowQueryService,
    private val enableAutomaticVersionUpgrade: Boolean,
    private val requiredCompatibility: VersionCompatibility,
    private val compatibilityTester: CompatibilityTester,
) : StepService {
    fun create(workflow: Workflow<*>, invokableStepDefinition: InvokableStepDefinition): StepCreationResult {
        stepDefinitionVersionRecorder.record(invokableStepDefinition.definition)

        val step = stepActivationService.activateInitial(
            workflow,
            invokableStepDefinition,
            StepIdentifier(persistence.randomId())
        )

        createData(step)
        eventService.publish(StepCreatedEvent(step))

        val onCreatedAutomations = step.definition.onCreatedAutomations.map { it.createAutomation(step) }

        return StepCreationResult(
            step,
            onCreatedAutomations
        )
    }

    private fun createData(step: Step): StepData {
        return persistence.create(
            StepData(
                step.identifier.value,
                step.workflow.identifier.value,
                step.definition.kind.value,
                step.definition.version.version,
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
        return persistence.findForWorkflow(workflow.identifier)
            .map { stepActivationService.activateFromPersistence(workflow, it) }
    }

    override fun <TWorkflowModel> findSteps(workflow: Workflow<TWorkflowModel>, query: StepQuery): Page<Step> {
        return persistence.findForWorkflow(
            workflow.identifier,
            query.toDataQuery()
        ).map { stepActivationService.activateFromPersistence(workflow, it) }
    }

    override fun findSteps(query: StepQuery): Page<Step> {
        val page = persistence.findAll(query.toDataQuery())
        val stepsByWorkflow = page.items.groupBy { step -> step.workflowId }
        val workflows = workflowQueryService.getAll(
            Query.withFilter(
                WorkflowFilter<Any?>(
                    id = Filter.anyOf(stepsByWorkflow.keys),
                    model = null,
                    modelType = null
                )
            )
        ).items.associateBy { it.identifier.value }

        return page.map { step ->
            stepActivationService.activateFromPersistence(
                workflows[step.workflowId]
                    ?: throw WorkflowNotFoundException(WorkflowIdentifier(step.workflowId)),
                step
            )
        }
    }

    override fun <TWorkflowModel> findStep(workflow: Workflow<TWorkflowModel>, stepIdentifier: StepIdentifier): Step? {
        return persistence.findForWorkflowAndId(workflow.identifier, stepIdentifier)
            ?.let { stepActivationService.activateFromPersistence(workflow, it) }
    }

    override fun setMetadata(step: Step, key: String, value: Any?): Step {
        val stepData = persistence.findForWorkflowAndId(
            step.workflow.identifier,
            step.identifier
        )!!
        val metadata = stepData.metadata.toMutableMap()
        if (value == null) {
            metadata.remove(key)
        } else {
            metadata[key] = value
        }
        return saveChanges(
            step,
            stepData.copy(
                metadata = metadata
            )
        )
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
        val stepData = persistence.findForWorkflowAndId(step.workflow.identifier, step.identifier)!!
        if (step.status == status) {
            return step
        }
        return saveChanges(step, stepData.withState(status))
    }

    fun saveChanges(step: Step): Step {
        val oldVersion = persistence.findForWorkflowAndId(step.workflow.identifier, step.identifier)!!
        val newVersion = applyVersionUpgrade(
            step,
            oldVersion.withData(fetchData(step)),
        )

        if (!changeDetector.hasChanged(oldVersion, newVersion)) {
            Logger.debug(
                "Step '{}' of workflow '{}' will not be persisted, because it didn't change.",
                step.identifier.value,
                step.workflow.identifier.value
            )
            return step
        }

        return saveChanges(step, newVersion)
    }

    private fun applyVersionUpgrade(
        step: Step,
        data: StepData,
    ): StepData {
        if (!enableAutomaticVersionUpgrade) return data
        if (data.version == step.definition.version.version) return data
        
        val actualCompatibility = compatibilityTester.checkCompatibility(step.version, step.definition.version)
        if (!requiredCompatibility.isSatisfiedBy(actualCompatibility)) {
            Logger.warn(
                "Skip updating step #{} from '{}' to '{}', as the compatibility {} doesn't satisfy the required compatibility of {}.",
                step.identifier.value,
                step.version.version,
                step.definition.version.version,
                actualCompatibility,
                requiredCompatibility
            )
            return data
        }

        Logger.info(
            "Automatically upgrading version of step #{} from '{}' to '{}'.",
            step.identifier.value,
            data.version,
            step.definition.version.version
        )
        return data.copy(version = step.definition.version.version)
    }

    private fun saveChanges(step: Step, data: StepData): Step {
        val updatedStep = stepActivationService.activateFromPersistence(step.workflow, persistence.save(data))
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