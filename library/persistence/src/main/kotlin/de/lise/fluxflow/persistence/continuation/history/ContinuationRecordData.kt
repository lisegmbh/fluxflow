package de.lise.fluxflow.persistence.continuation.history

import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.history.ContinuationRecord
import de.lise.fluxflow.api.continuation.history.ContinuationRecordIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import java.time.Instant

/**
 * A [ContinuationRecordData] describes a continuation that took place during workflow execution.
 */
data class ContinuationRecordData(
    /**
     * The identifier of a persisted data entry.
     * Must be `null` for objects yet to be persisted and not `null` for already persisted objects. 
     */
    val id: String?,
    val workflowId: String,
    val timeOfOccurrence: Instant,
    val type: ContinuationType,
    /**
     * Describes the originating workflow object, that started the continuation.
     * Might be `null`, if there is no originating object (e.g., when a workflow is started).
     */
    val originatingObject: WorkflowObjectReference?,
    /**
     * Describes the workflow object that resulted from the continuation.
     * Might be `null`, if there is no resulting workflow object (e.g., for [Continuation.none]).
     */
    val targetObject: WorkflowObjectReference? 
) {

    fun toDomainObject(): ContinuationRecord {
        return ContinuationRecord(
            id = ContinuationRecordIdentifier(id!!),
            workflowIdentifier = WorkflowIdentifier(workflowId),
            timeOfOccurrence = timeOfOccurrence,
            type = type,
            originatingObject = originatingObject,
            targetObject = targetObject
        )
    }
}