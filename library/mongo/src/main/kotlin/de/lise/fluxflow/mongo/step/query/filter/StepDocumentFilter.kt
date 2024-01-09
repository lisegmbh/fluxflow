package de.lise.fluxflow.mongo.step.query.filter

import de.lise.fluxflow.api.step.Status
import de.lise.fluxflow.mongo.query.filter.DocumentFilter
import de.lise.fluxflow.mongo.query.filter.MongoFilter
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.persistence.step.query.filter.StepDataFilter
import org.springframework.data.mongodb.core.query.Criteria

class StepDocumentFilter(
    val id: MongoFilter<String>?,
    val kind: MongoFilter<String>?,
    val status: MongoFilter<Status>?,
    val metadata: MongoFilter<Map<String, Any>>?,
    val workflowId: MongoFilter<String>?
): DocumentFilter<StepDocument> {
    constructor(
        stepDataFilter: StepDataFilter
    ) : this(
        stepDataFilter.id?.let { MongoFilter.fromDomainFilter(it) },
        stepDataFilter.kind?.let { MongoFilter.fromDomainFilter(it) },
        stepDataFilter.status?.let { MongoFilter.fromDomainFilter(it) },
        stepDataFilter.metadata?.let { MongoFilter.fromDomainFilter(it) },
        workflowId = null
    )

    override fun toCriteria(): Criteria {
        val criteria = mutableListOf<Criteria>()

        if (id != null) {
            criteria.add(id.apply("id"))
        }
        if (kind != null) {
            criteria.add(kind.apply("kind"))
        }
        if (status != null) {
            criteria.add(status.apply("status"))
        }
        if(workflowId != null) {
            criteria.add(workflowId.apply("workflowId"))
        }

        return when(criteria.size) {
            0 -> Criteria()
            1 -> criteria.first()
            else -> Criteria().andOperator(criteria)
        }
    }
}