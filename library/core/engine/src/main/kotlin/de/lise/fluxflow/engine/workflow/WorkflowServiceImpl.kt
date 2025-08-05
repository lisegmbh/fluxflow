package de.lise.fluxflow.engine.workflow

import de.lise.fluxflow.api.ReferredWorkflowObject
import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.workflow.*
import de.lise.fluxflow.api.workflow.query.WorkflowQuery
import de.lise.fluxflow.query.pagination.Page
import kotlin.reflect.KClass

class WorkflowServiceImpl(
    private val workflowStarterService: WorkflowStarterService,
    private val workflowQueryService: WorkflowQueryService,
    private val workflowUpdateService: WorkflowUpdateService,
    private val workflowRemovalService: WorkflowRemovalService
) : WorkflowService {
    override fun <TWorkflowModel : Any, TContinuation> start(
        workflowModel: TWorkflowModel,
        continuation: Continuation<TContinuation>,
        originatingObject: ReferredWorkflowObject<*>?,
        forcedId: WorkflowIdentifier?
    ): Workflow<TWorkflowModel> {
        return workflowStarterService.start(
            workflowModel,
            continuation,
            originatingObject,
            forcedId
        )
    }

    override fun getAll(): List<Workflow<*>> {
        return workflowQueryService.getAll()
    }

    override fun <TWorkflowModel : Any> getAll(workflowType: KClass<TWorkflowModel>): List<Workflow<TWorkflowModel>> {
        return workflowQueryService.getAll(workflowType)
    }

    override fun getAll(query: WorkflowQuery<*>): Page<Workflow<*>> {
        return workflowQueryService.getAll(query)
    }

    override fun <TWorkflowModel : Any> getAll(
        modelType: KClass<TWorkflowModel>,
        query: WorkflowQuery<TWorkflowModel>
    ): Page<Workflow<TWorkflowModel>> {
        return workflowQueryService.getAll(modelType, query)
    }

    override fun <TWorkflowModel> get(identifier: WorkflowIdentifier): Workflow<TWorkflowModel> {
        return workflowQueryService.get(identifier)
    }

    override fun <TWorkflowModel> getOrNull(identifier: WorkflowIdentifier): Workflow<TWorkflowModel>? {
        return workflowQueryService.getOrNull(identifier)
    }

    override fun <TWorkflowModel> saveChanges(workflow: Workflow<TWorkflowModel>): Workflow<TWorkflowModel> {
        return workflowUpdateService.saveChanges(workflow)
    }

    override fun delete(identifierToDelete: WorkflowIdentifier) {
        return workflowRemovalService.delete(identifierToDelete)
    }
}