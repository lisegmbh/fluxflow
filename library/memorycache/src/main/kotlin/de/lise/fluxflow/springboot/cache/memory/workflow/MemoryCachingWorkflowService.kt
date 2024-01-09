package de.lise.fluxflow.springboot.cache.memory.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.api.workflow.query.WorkflowQuery
import de.lise.fluxflow.query.pagination.Page
import kotlin.reflect.KClass

class MemoryCachingWorkflowService(
    private val workflowService: WorkflowService,
    private val workflowCache: WorkflowCache
) : WorkflowService {


    override fun getAll(): List<Workflow<*>> {
        return workflowService.getAll()
            .map { workflowCache.get(it) }
    }

    override fun <TWorkflowModel : Any> getAll(workflowType: KClass<TWorkflowModel>): List<Workflow<TWorkflowModel>> {
        return workflowService.getAll(workflowType)
            .map(workflowCache::get)
    }


    override fun getAll(query: WorkflowQuery<*>): Page<Workflow<*>> {
        return workflowService.getAll(query)
            .map { workflowCache.get(it) }
    }

    override fun <TWorkflowModel : Any> getAll(
        modelType: KClass<TWorkflowModel>,
        query: WorkflowQuery<TWorkflowModel>
    ): Page<Workflow<TWorkflowModel>> {
        return workflowService.getAll(modelType, query)
            .map { workflowCache.get(it) }
    }

    override fun <TWorkflowModel> get(identifier: WorkflowIdentifier): Workflow<TWorkflowModel> {
        @Suppress("UNCHECKED_CAST")
        return workflowCache.get(identifier) {
            workflowService.get<TWorkflowModel>(it)
        } as Workflow<TWorkflowModel>
    }

    override fun delete(identifierToDelete: WorkflowIdentifier) {
        workflowCache.discard(identifierToDelete)
        return workflowService.delete(identifierToDelete)
    }

    override fun replace(identifierToDelete: WorkflowIdentifier) {
        workflowCache.discard(identifierToDelete)
        return workflowService.replace(identifierToDelete)
    }
}