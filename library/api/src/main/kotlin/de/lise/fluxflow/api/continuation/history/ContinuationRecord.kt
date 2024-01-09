package de.lise.fluxflow.api.continuation.history

import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import java.time.Instant

data class ContinuationRecord(
    val id: ContinuationRecordIdentifier,
    val workflowIdentifier: WorkflowIdentifier,
    val timeOfOccurrence: Instant,
    val type: ContinuationType,
    val originatingObject: WorkflowObjectReference?,
    val targetObject: WorkflowObjectReference?
)