package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowNotFoundException
import de.lise.fluxflow.api.workflow.WorkflowQueryService
import de.lise.fluxflow.api.workflow.query.WorkflowQuery
import de.lise.fluxflow.api.workflow.query.filter.WorkflowFilter
import de.lise.fluxflow.api.workflow.query.sort.WorkflowSort
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.persistence.workflow.query.toDataQuery
import de.lise.fluxflow.query.Query
import de.lise.fluxflow.query.filter.Filter
import de.lise.fluxflow.query.pagination.Page
import kotlin.reflect.KClass

class WorkflowQueryServiceImpl(
    private val persistence: WorkflowPersistence,
    private val activationService: WorkflowActivationService
) : WorkflowQueryService {
    override fun getAll(): List<Workflow<*>> {
        return persistence.findAll()
            .map {
                activationService.activate<Any?>(it)
            }
    }

    override fun <TWorkflowModel : Any> getAll(workflowType: KClass<TWorkflowModel>): List<Workflow<TWorkflowModel>> {
        return getAll(
            workflowType,
            Query.empty<WorkflowFilter<TWorkflowModel>, WorkflowSort<TWorkflowModel>>()
                .withFilter(
                    WorkflowFilter(
                        id = null,
                        model = null,
                        modelType = Filter.ofType(workflowType)
                    )
                )
        ).items
    }

    override fun getAll(query: WorkflowQuery<*>): Page<Workflow<*>> {
        return persistence.findAll(
            query.toDataQuery()
        ).map {
            activationService.activate<Any?>(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <TWorkflowModel : Any> getAll(
        modelType: KClass<TWorkflowModel>,
        query: WorkflowQuery<TWorkflowModel>
    ): Page<Workflow<TWorkflowModel>> {
        return getAll(
            (query as WorkflowQuery<*>).mapFilter {
                (it ?: WorkflowFilter.empty<TWorkflowModel>())
                    .fallbackModelTypeFilter(
                        Filter.ofType(modelType)
                    )
            }
        ).map {
            it as Workflow<TWorkflowModel>
        }
    }

    override fun <TWorkflowModel> get(identifier: WorkflowIdentifier): Workflow<TWorkflowModel> {
        return persistence.find(identifier)
            ?.let {
                activationService.activate(it)
            } ?: throw WorkflowNotFoundException(identifier)
    }
}