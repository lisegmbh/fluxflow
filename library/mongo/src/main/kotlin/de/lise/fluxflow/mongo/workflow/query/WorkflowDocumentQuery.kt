package de.lise.fluxflow.mongo.workflow.query

import de.lise.fluxflow.mongo.workflow.query.filter.WorkflowDocumentFilter
import de.lise.fluxflow.mongo.workflow.query.sort.WorkflowDocumentSort
import de.lise.fluxflow.persistence.workflow.query.WorkflowDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.pagination.PaginationRequest
import org.springframework.data.domain.Sort

class WorkflowDocumentQuery(
    filter: WorkflowDocumentFilter?,
    sort: List<Sort>,
    page: PaginationRequest?
) : Query<WorkflowDocumentFilter, Sort>(filter, sort, page) {
    constructor(
        query: WorkflowDataQuery
    ) : this(
        query.filter?.let { WorkflowDocumentFilter(it) },
        query.sort.map { WorkflowDocumentSort.toMongoSort(it) },
        query.page
    )
}