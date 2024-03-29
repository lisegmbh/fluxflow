package de.lise.fluxflow.engine.continuation.history

import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.history.ContinuationHistoryService
import de.lise.fluxflow.api.continuation.history.ContinuationRecord
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQuery
import de.lise.fluxflow.api.continuation.history.query.filter.ContinuationRecordFilter
import de.lise.fluxflow.api.continuation.history.query.filter.WorkflowObjectReferenceFilter
import de.lise.fluxflow.api.continuation.history.query.sort.ContinuationRecordSort
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.continuation.history.query.toDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.filter.Filter
import de.lise.fluxflow.query.pagination.Page
import java.time.Clock

class ContinuationHistoryServiceImpl(
    private val continuationRecordPersistence: ContinuationRecordPersistence,
    private val stepService: StepService,
    private val clock: Clock,
) : ContinuationHistoryService {
    fun create(
        workflowId: WorkflowIdentifier,
        origin: WorkflowObjectReference?,
        continuation: Continuation<*>,
        target: WorkflowObjectReference?
    ) {
        continuationRecordPersistence.create(
            ContinuationRecordData(
                id = null,
                workflowId = workflowId.value,
                timeOfOccurrence = clock.instant(),
                continuation.type,
                origin,
                target
            )
        )
    }

    override fun findAll(query: ContinuationRecordQuery): Page<ContinuationRecord> {
        return continuationRecordPersistence.findAll(
            query.toDataQuery()
        ).map {
            it.toDomainObject()
        }
    }

    override fun findPreviousStep(currentStep: Step): Step? {
        val ref = findAll(
            Query.withFilter<ContinuationRecordFilter, ContinuationRecordSort>(
                ContinuationRecordFilter(
                    workflowIdentifier = Filter.eq(currentStep.workflow.identifier.value),
                    type = Filter.eq(ContinuationType.Step),
                    originatingObject = WorkflowObjectReferenceFilter(
                        kind = Filter.eq(WorkflowObjectKind.Step)
                    ),
                    targetObject = WorkflowObjectReferenceFilter(
                        kind = Filter.eq(WorkflowObjectKind.Step),
                        objectId = Filter.eq(currentStep.identifier.value)
                    )
                )
            ).withPage(0, 1)
        ).items.firstOrNull() ?: return null

        return stepService.findStep(currentStep.workflow, StepIdentifier(ref.originatingObject!!.objectId))
    }

    override fun deleteAllForWorkflow(identifierToDelete: WorkflowIdentifier) {
        continuationRecordPersistence.deleteAllForWorkflow(identifierToDelete)
    }
}