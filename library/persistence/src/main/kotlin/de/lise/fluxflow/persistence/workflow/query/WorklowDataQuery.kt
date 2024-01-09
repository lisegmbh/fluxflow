package de.lise.fluxflow.persistence.workflow.query

import de.lise.fluxflow.api.workflow.query.WorkflowQuery
import de.lise.fluxflow.persistence.workflow.query.filter.WorkflowDataFilter
import de.lise.fluxflow.persistence.workflow.query.sort.WorkflowDataSort
import de.lise.fluxflow.query.Query

typealias WorkflowDataQuery = Query<WorkflowDataFilter, WorkflowDataSort>

fun WorkflowQuery<*>.toDataQuery(): WorkflowDataQuery {
    return WorkflowDataQuery(
        this.filter?.let { WorkflowDataFilter(it) },
        this.sort.map {
            WorkflowDataSort.fromDomainSort(it)
        },
        this.page
    )
}