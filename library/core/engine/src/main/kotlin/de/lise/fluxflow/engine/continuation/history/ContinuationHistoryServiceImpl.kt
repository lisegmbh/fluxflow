package de.lise.fluxflow.engine.continuation.history

import de.fluxflow.flowquery.expression.ExpressionExtensions.Logical.and
import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.history.ContinuationHistoryService
import de.lise.fluxflow.api.continuation.history.ContinuationRecord
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQuery
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQueryable
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQueryable.Companion.originatingObject
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQueryable.Companion.targetObject
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQueryable.Companion.type
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQueryable.Companion.workflowIdentifier
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.continuation.history.query.toDataQuery
import de.lise.fluxflow.query.pagination.Page
import java.time.Clock

class ContinuationHistoryServiceImpl(
    private val continuationRecordPersistence: ContinuationRecordPersistence,
    private val stepService: StepService,
    private val clock: Clock,
    private val queryMapper: QueryMapper<ContinuationRecordQueryable, ContinuationRecordData>
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

    override fun findAll(
        query: FlowQuery<ContinuationRecordQueryable, ContinuationRecordQueryable>
    ): Page<ContinuationRecord> {
        return continuationRecordPersistence.findAll(
            queryMapper.map(query)
        ).map { 
            it.toDomainObject()
        }
    }

    override fun findPreviousStep(currentStep: Step): Step? {
        val ref = findAll {
            where {
                workflowIdentifier.isEqual(currentStep.workflow.identifier) and 
                    type.isEqual(ContinuationType.Step) and
                    originatingObject.get(WorkflowObjectReference::kind).isEqual(WorkflowObjectKind.Step) and
                    targetObject.allTrue(
                        {
                            get(WorkflowObjectReference::kind).isEqual(WorkflowObjectKind.Step)
                        },
                        {
                            get(WorkflowObjectReference::objectId).isEqual(currentStep.identifier.value)    
                        }
                    )
            }.paged(0,1)
        }.items.firstOrNull() ?: return null

        return stepService.findStep(currentStep.workflow, StepIdentifier(ref.originatingObject!!.objectId))
    }

    override fun deleteAllForWorkflow(identifierToDelete: WorkflowIdentifier) {
        continuationRecordPersistence.deleteAllForWorkflow(identifierToDelete)
    }
}