package de.lise.fluxflow.test.persistence.continuation.history.query.filter

import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.query.filter.ContinuationRecordDataFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate
import java.time.Instant

class ContinuationRecordTestFilter(
    private val id: InMemoryFilter<String>?,
    private val workflowId: InMemoryFilter<String>?,
    private val timeOfOccurrence: InMemoryFilter<Instant>?,
    private val type: InMemoryFilter<ContinuationType>?,
    private val originatingObject: InMemoryFilter<WorkflowObjectReference>?,
    private val targetObject: InMemoryFilter<WorkflowObjectReference>?
) {
    constructor(
        filter: ContinuationRecordDataFilter
    ): this(
        filter.id?.let { InMemoryFilter.fromDomainFilter(it) },
        filter.workflowId?.let { InMemoryFilter.fromDomainFilter(it) },
        filter.timeOfOccurrence?.let { InMemoryFilter.fromDomainFilter(it) },
        filter.type?.let { InMemoryFilter.fromDomainFilter(it) },
        filter.originatingObject?.let { InMemoryWorkflowObjectReferenceFilter(it) },
        filter.targetObject?.let { InMemoryWorkflowObjectReferenceFilter(it) }
    )

    fun toPredicate(): InMemoryPredicate<ContinuationRecordData> {
        var result = InMemoryPredicate<ContinuationRecordData> {
            true
        }

        id?.let {
            result = result.and(it.toPredicate().mapping { data -> data.id!! })
        }
        workflowId?.let {
            result = result.and(it.toPredicate().mapping { data -> data.workflowId })
        }
        timeOfOccurrence?.let {
            result = result.and(it.toPredicate().mapping { data -> data.timeOfOccurrence })
        }
        type?.let {
            result = result.and(it.toPredicate().mapping { data -> data.type })
        }
        originatingObject?.let {
            result = result.and(it.toPredicate().optionalMapping { data -> data.originatingObject })
        }
        targetObject?.let {
            result = result.and(it.toPredicate().optionalMapping { data -> data.targetObject })
        }
        return result
    }

}