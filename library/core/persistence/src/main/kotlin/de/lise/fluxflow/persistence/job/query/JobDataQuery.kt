package de.lise.fluxflow.persistence.job.query

import de.lise.fluxflow.api.job.query.JobQuery
import de.lise.fluxflow.persistence.job.query.filter.JobDataFilter
import de.lise.fluxflow.persistence.job.query.sort.JobDataSort
import de.lise.fluxflow.query.Query

typealias JobDataQuery = Query<JobDataFilter, JobDataSort>

fun JobQuery.toDataQuery(): JobDataQuery {
    return JobDataQuery(
        this.filter?.let { JobDataFilter(it) },
        this.sort.map {
            JobDataSort.fromDomainSort(it)
        },
        this.page
    )
}