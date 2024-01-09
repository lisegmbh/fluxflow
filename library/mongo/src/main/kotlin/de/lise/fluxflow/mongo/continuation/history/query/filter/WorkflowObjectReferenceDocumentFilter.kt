package de.lise.fluxflow.mongo.continuation.history.query.filter

import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.history.query.filter.WorkflowObjectReferenceFilter
import de.lise.fluxflow.mongo.query.filter.MongoFilter
import org.springframework.data.mongodb.core.query.Criteria

class WorkflowObjectReferenceDocumentFilter(
  val kind: MongoFilter<WorkflowObjectKind>?,
  val objectId: MongoFilter<String>?
): MongoFilter<WorkflowObjectReference>  {
    constructor(
        filter: WorkflowObjectReferenceFilter
    ) : this(
        filter.kind?.let { MongoFilter.fromDomainFilter(it) },
        filter.objectId?.let { MongoFilter.fromDomainFilter(it) }
    )

    override fun apply(path: String): Criteria {
        val criteria = mutableListOf<Criteria>()
        kind?.let {
            criteria.add(it.apply("${path}.kind"))
        }
        objectId?.let {
            criteria.add(it.apply("${path}.objectId"))
        }

        return when(criteria.size) {
            0 -> Criteria()
            1 -> criteria.first()
            else -> Criteria().andOperator(criteria)
        }
    }
}