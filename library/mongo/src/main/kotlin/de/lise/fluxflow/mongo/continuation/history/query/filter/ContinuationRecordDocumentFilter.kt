package de.lise.fluxflow.mongo.continuation.history.query.filter

import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.query.filter.DocumentFilter
import de.lise.fluxflow.mongo.query.filter.MongoFilter
import de.lise.fluxflow.persistence.continuation.history.query.filter.ContinuationRecordDataFilter
import org.springframework.data.mongodb.core.query.Criteria
import java.time.Instant

class ContinuationRecordDocumentFilter(
    val id: MongoFilter<String>?,
    val workflowId: MongoFilter<String>?,
    val timeOfOccurrence: MongoFilter<Instant>?,
    val type: MongoFilter<ContinuationType>?,
    val originatingObject: WorkflowObjectReferenceDocumentFilter?,
    val targetObject: WorkflowObjectReferenceDocumentFilter?
) : DocumentFilter<ContinuationRecordDocument> {
    constructor(
        filter: ContinuationRecordDataFilter
    ) : this(
        filter.id?.let { MongoFilter.fromDomainFilter(it) },
        filter.workflowId?.let { MongoFilter.fromDomainFilter(it) },
        filter.timeOfOccurrence?.let { MongoFilter.fromDomainFilter(it) },
        filter.type?.let { MongoFilter.fromDomainFilter(it) },
        filter.originatingObject?.let { WorkflowObjectReferenceDocumentFilter(it) },
        filter.targetObject?.let { WorkflowObjectReferenceDocumentFilter(it) }
    )

    override fun toCriteria(): Criteria {
        val criteria = mutableListOf<Criteria>()
        id?.let {
            criteria.add(it.apply("id"))
        }
        workflowId?.let {
            criteria.add(it.apply("workflowId"))
        }
        timeOfOccurrence?.let {
            criteria.add(it.apply("timeOfOccurrence"))
        }
        type?.let {
            criteria.add(it.apply("type"))
        }
        originatingObject?.let {
            criteria.add(it.apply("originatingObject"))
        }
        targetObject?.let {
            criteria.add(it.apply("targetObject"))
        }

        return when (criteria.size) {
            0 -> Criteria()
            1 -> criteria.first()
            else -> Criteria().andOperator(criteria)
        }
    }
}