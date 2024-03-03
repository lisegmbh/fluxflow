package de.lise.fluxflow.test.persistence.workflow.query.filter

import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.query.filter.WorkflowDataFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryFilter
import de.lise.fluxflow.query.inmemory.filter.InMemoryPredicate

class WorkflowTestFilter(
    private val id: InMemoryFilter<String>?,
    private val model: InMemoryFilter<Any?>?,
    private val modelType: InMemoryFilter<Any?>?
) {
    @Suppress("UNCHECKED_CAST")
    constructor(filter: WorkflowDataFilter) : this(
        filter.id?.let { InMemoryFilter.fromDomainFilter(it) },
        filter.model?.let { InMemoryFilter.fromDomainFilter(it) as InMemoryFilter<Any?> },
        filter.modelType?.let { InMemoryFilter.fromDomainFilter(it) as InMemoryFilter<Any?> },
    )


    fun toPredicate(): InMemoryPredicate<WorkflowData> {
        var result = InMemoryPredicate<WorkflowData> {
            true
        }

        if (id != null) {
            result = result.and(id.toPredicate().mapping { it.id })
        }
        if (model != null) {
            result = result.and(model.toPredicate().mapping { it.model })
        }
        if (modelType != null) {
            result = result.and(modelType.toPredicate().mapping { it.model })
        }

        return result
    }

}