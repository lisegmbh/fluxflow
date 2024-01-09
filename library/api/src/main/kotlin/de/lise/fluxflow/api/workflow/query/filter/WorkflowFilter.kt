package de.lise.fluxflow.api.workflow.query.filter

import de.lise.fluxflow.api.Api
import de.lise.fluxflow.query.filter.Filter
import kotlin.reflect.KClass

data class WorkflowFilter<TWorkflowModel>(
    val id: Filter<String>?,
    val model: Filter<TWorkflowModel>?,
    val modelType: Filter<KClass<*>>?,
) {
    @Api
    fun withIdFilter(id: Filter<String>): WorkflowFilter<TWorkflowModel> {
        return WorkflowFilter(
            id,
            model,
            modelType
        )
    }

    @Api
    fun withModelFilter(model: Filter<TWorkflowModel>): WorkflowFilter<TWorkflowModel> {
        return WorkflowFilter(
            id,
            model,
            modelType
        )
    }

    fun andModelFilter(model: Filter<TWorkflowModel>): WorkflowFilter<TWorkflowModel> {
        if (this.model == null) {
            return withModelFilter(model)
        }
        return WorkflowFilter(
            id,
            this.model.and(model),
            modelType
        )
    }

    fun withModelTypeFilter(modelType: Filter<KClass<*>>): WorkflowFilter<TWorkflowModel> {
        return WorkflowFilter(
            id,
            model,
            modelType
        )
    }

    /**
     * Returns a new [WorkflowFilter] that uses the given [modelType],
     * if no workflow type filter has been defined before.
     * Otherwise, the current instance is returned unmodified.
     * @return a new filter with the specified model filer if previously undefined - otherwise the current instance.
     */
    fun fallbackModelTypeFilter(modelType: Filter<KClass<*>>): WorkflowFilter<TWorkflowModel> {
        if(this.modelType == null) {
            return WorkflowFilter(
                id,
                model,
                modelType
            )
        }
        return this
    }

    companion object {
        fun <TWorkflowModel> empty(): WorkflowFilter<TWorkflowModel> {
            return WorkflowFilter(null, null, null)
        }
    }

}