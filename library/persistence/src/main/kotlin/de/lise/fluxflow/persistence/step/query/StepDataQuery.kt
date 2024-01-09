package de.lise.fluxflow.persistence.step.query

import de.lise.fluxflow.api.step.query.StepQuery
import de.lise.fluxflow.persistence.step.query.filter.StepDataFilter
import de.lise.fluxflow.persistence.step.query.sort.StepDataSort
import de.lise.fluxflow.query.Query

typealias StepDataQuery = Query<StepDataFilter, StepDataSort>

fun StepQuery.toDataQuery(): StepDataQuery {
    return StepDataQuery(
        this.filter?.let { StepDataFilter(it) },
        this.sort.map { StepDataSort.fromDomainSort(it) },
        this.page
    )
}