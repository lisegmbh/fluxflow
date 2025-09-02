package de.lise.fluxflow.test.persistence.step.query.filter

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.query.filter.StepDataFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate

class StepTestFilter(
    private val id: InMemoryFilter<String>?,
    private val kind: InMemoryFilter<String>?,
    private val version: InMemoryFilter<String>?,
    private val status: InMemoryFilter<Status>?
) {
    constructor(stepFilter: StepDataFilter) : this(
        stepFilter.id?.let { InMemoryFilter.fromDomainFilter(it) },
        stepFilter.kind?.let { InMemoryFilter.fromDomainFilter(it) },
        stepFilter.version?.let { InMemoryFilter.fromDomainFilter(it) },
        stepFilter.status?.let { InMemoryFilter.fromDomainFilter(it) }
    )

    fun toPredicate(): InMemoryPredicate<StepData> {
        var result = InMemoryPredicate<StepData> {
            true
        }

        if (id != null) {
            result = result.and(id.toPredicate().mapping { it.id })
        }
        if (kind != null) {
            result = result.and(kind.toPredicate().mapping { it.kind })
        }
        if (version != null) {
            result = result.and(version.toPredicate().mapping { it.version ?: "" })
        }
        if (status != null) {
            result = result.and(status.toPredicate().mapping { it.status })
        }

        return result
    }
}