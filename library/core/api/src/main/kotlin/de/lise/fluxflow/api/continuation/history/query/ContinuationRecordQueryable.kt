package de.lise.fluxflow.api.continuation.history.query

import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.history.ContinuationRecordIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import java.time.Instant

interface ContinuationRecordQueryable {
    val id: ContinuationRecordIdentifier
    val workflowIdentifier: WorkflowIdentifier
    val timeOfOccurrence: Instant
    val type: ContinuationType
    val originatingObject: WorkflowObjectReference?
    val targetObject: WorkflowObjectReference?
}