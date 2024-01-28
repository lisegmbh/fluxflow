package de.lise.fluxflow.api.workflow

import de.lise.fluxflow.api.workflow.query.WorkflowQuery
import de.lise.fluxflow.query.pagination.Page
import kotlin.reflect.KClass

interface WorkflowService {
    fun getAll(): List<Workflow<*>>
    fun <TWorkflowModel : Any> getAll(workflowType: KClass<TWorkflowModel>): List<Workflow<TWorkflowModel>>
    fun getAll(query: WorkflowQuery<*>): Page<Workflow<*>>
    fun <TWorkflowModel : Any> getAll(
        modelType: KClass<TWorkflowModel>,
        query: WorkflowQuery<TWorkflowModel>
    ): Page<Workflow<TWorkflowModel>>

    fun <TWorkflowModel> get(identifier: WorkflowIdentifier): Workflow<TWorkflowModel>

    /**
     * Deletes the workflow with the given identifier and all of its related steps and jobs.
     *
     * You do *not* need to cancel any scheduled jobs.
     *
     * @param identifierToDelete the id of the workflow to delete
     */
    fun delete(identifierToDelete: WorkflowIdentifier)

    /**
     * Deletes the workflow with the given identifier and all of its related steps.
     *
     * @param identifierToDelete the id of the workflow to delete
     */
    @Deprecated("This method was never intended to be part of the public API and should only be used internally. It will be removed or replaced in an upcoming version.")
    fun replace(identifierToDelete: WorkflowIdentifier)
}