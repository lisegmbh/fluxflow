package de.lise.fluxflow.api.continuation.history.query.filter

import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.query.filter.Filter

data class WorkflowObjectReferenceFilter(
    val kind: Filter<WorkflowObjectKind>? = null,
    val objectId: Filter<String>? = null
)