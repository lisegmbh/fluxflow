package de.lise.fluxflow.persistence.continuation.history.query.filter

import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.history.query.filter.ContinuationRecordFilter
import de.lise.fluxflow.api.continuation.history.query.filter.WorkflowObjectReferenceFilter
import de.lise.fluxflow.query.filter.Filter
import java.time.Instant

data class ContinuationRecordDataFilter(
    val id: Filter<String>?,
    val workflowId: Filter<String>?,
    val timeOfOccurrence: Filter<Instant>?,
    val type: Filter<ContinuationType>?,
    val originatingObject: WorkflowObjectReferenceFilter?,
    val targetObject: WorkflowObjectReferenceFilter?
) {
    constructor(
        filter: ContinuationRecordFilter
    ) : this(
        filter.id?.value,
        filter.workflowIdentifier,
        filter.timeOfOccurrence,
        filter.type,
        filter.originatingObject,
        filter.targetObject
    )
}