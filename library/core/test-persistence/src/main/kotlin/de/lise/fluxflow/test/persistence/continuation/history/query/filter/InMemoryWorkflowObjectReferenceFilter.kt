package de.lise.fluxflow.test.persistence.continuation.history.query.filter

import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.history.query.filter.WorkflowObjectReferenceFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate

data class InMemoryWorkflowObjectReferenceFilter(
    val kind: InMemoryFilter<WorkflowObjectKind>?,
    val objectId: InMemoryFilter<String>?
): InMemoryFilter<WorkflowObjectReference> {
    constructor(
        filter: WorkflowObjectReferenceFilter
    ) : this(
        filter.kind?.let { InMemoryFilter.fromDomainFilter(it) },
        filter.objectId?.let { InMemoryFilter.fromDomainFilter(it) }
    )

    override fun toPredicate(): InMemoryPredicate<WorkflowObjectReference> {
        return listOfNotNull(
            InMemoryPredicate { true },
            kind?.toPredicate()?.mapping { it.kind },
            objectId?.toPredicate()?.mapping<WorkflowObjectReference> { it.objectId }
        ).reduce { acc, next ->
            acc.and(next)
        }
    }
}