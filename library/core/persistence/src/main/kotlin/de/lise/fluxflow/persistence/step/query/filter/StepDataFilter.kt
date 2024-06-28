package de.lise.fluxflow.persistence.step.query.filter

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.api.step.query.filter.StepFilter
import de.lise.fluxflow.query.filter.Filter

data class StepDataFilter(
    val id: Filter<String>?,
    val kind: Filter<String>?,
    val version: Filter<String>?,
    val status: Filter<Status>?,
    val metadata: Filter<Map<String, Any>>?
) {
    constructor(stepFilter: StepFilter) : this(
        stepFilter.id?.value,
        stepFilter.definition?.kind?.value,
        stepFilter.version?.version,
        stepFilter.status,
        stepFilter.metadata
    )
}