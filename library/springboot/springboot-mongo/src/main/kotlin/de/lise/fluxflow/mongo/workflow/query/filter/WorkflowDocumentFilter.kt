package de.lise.fluxflow.mongo.workflow.query.filter

import de.lise.fluxflow.mongo.query.filter.DocumentFilter
import de.lise.fluxflow.mongo.query.filter.MongoFilter
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.workflow.query.filter.WorkflowDataFilter
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KClass


class WorkflowDocumentFilter(
    private val id: MongoFilter<String>?,
    private val model: MongoFilter<Any>?,
    private val modelType: MongoFilter<KClass<*>>?,
) : DocumentFilter<WorkflowDocument> {
    @Suppress("UNCHECKED_CAST")
    constructor(
        workflowDataFilter: WorkflowDataFilter
    ) : this(
        workflowDataFilter.id?.let { MongoFilter.fromDomainFilter(it) },
        workflowDataFilter.model?.let {
            MongoFilter.fromDomainFilter(it) as MongoFilter<Any>
        },
        workflowDataFilter.modelType?.let { MongoFilter.fromDomainFilter(it) }
    )

    override fun toCriteria(): Criteria {
        val criteria = mutableListOf<Criteria>()

        if (id != null) {
            criteria.add(id.apply("id"))
        }
        if (model != null) {
            criteria.add(model.apply("model"))
        }
        if (modelType != null) {
            criteria.add(modelType.apply("modelType"))
        }

        return when (criteria.size) {
            0 -> Criteria()
            1 -> criteria.first()
            else -> Criteria().andOperator(criteria)
        }
    }
}