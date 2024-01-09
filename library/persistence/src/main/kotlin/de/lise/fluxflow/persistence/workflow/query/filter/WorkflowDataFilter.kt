package de.lise.fluxflow.persistence.workflow.query.filter

import de.lise.fluxflow.api.workflow.query.filter.WorkflowFilter
import de.lise.fluxflow.query.filter.Filter
import kotlin.reflect.KClass

data class WorkflowDataFilter(
    val id: Filter<String>?,
    val model: Filter<*>?,
    val modelType: Filter<KClass<*>>?,
) {
    constructor(workflowFilter: WorkflowFilter<*>) : this(
        workflowFilter.id,
        workflowFilter.model,
        workflowFilter.modelType
    )
}