package de.lise.fluxflow.api.continuation.history.query.filter

import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.query.filter.Filter
import java.time.Instant

data class ContinuationRecordFilter(
    val id: ContinuationRecordIdentifierFilter? = null,
    val workflowIdentifier: Filter<String>? = null,
    val timeOfOccurrence: Filter<Instant>? = null,
    val type: Filter<ContinuationType>? = null,
    val originatingObject: WorkflowObjectReferenceFilter? = null,
    val targetObject: WorkflowObjectReferenceFilter? = null
)

