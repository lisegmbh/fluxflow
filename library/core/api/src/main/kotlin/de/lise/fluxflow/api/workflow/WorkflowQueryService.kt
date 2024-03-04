package de.lise.fluxflow.api.workflow

import de.lise.fluxflow.api.workflow.query.WorkflowQuery
import de.lise.fluxflow.query.pagination.Page
import kotlin.reflect.KClass

/**
 * The [WorkflowQueryService] provides functionalities to get and query for existing workflows.
 *
 * In order to manipulate a workflow's state, use the [WorkflowUpdateService].
 * To start a new workflow, use the [WorkflowStarterService].
 * To remove a workflow, use the [WorkflowRemovalService].
 *
 * @see WorkflowUpdateService
 * @see WorkflowStarterService
 */
interface WorkflowQueryService {
    /**
     * Returns all workflows.
     *
     * **Warning**: This method should be used with caution, as it may return a large number of workflows.
     * Consider using the [getAll] method with a [WorkflowQuery] instead.
     *  @return a list of all workflows
     */
    fun getAll(): List<Workflow<*>>

    /**
     * Returns all workflows having a workflow model of the given type.
     *
     * **Warning**: This method should be used with caution, as it may return a large number of workflows.
     * Consider using the [getAll] method with a [WorkflowQuery] instead.
     * @param workflowType the type of the workflow model
     * @param TWorkflowModel the type of the workflow model
     * @return a list of all workflows with a model of the given type
     */
    fun <TWorkflowModel : Any> getAll(workflowType: KClass<TWorkflowModel>): List<Workflow<TWorkflowModel>>

    /**
     * Returns all workflows matching the given query.
     * @param query the query that should be applied to filter the workflows
     * @return a page of workflows matching the given query
     */
    fun getAll(query: WorkflowQuery<*>): Page<Workflow<*>>

    /**
     * Returns all workflows matching the given query.
     * @param modelType the type of the workflows' model
     * @param query the query that should be applied to filter the workflows
     * @param TWorkflowModel the type of the workflows' model
     * @return a page of workflows matching the given query
     */
    fun <TWorkflowModel : Any> getAll(
        modelType: KClass<TWorkflowModel>,
        query: WorkflowQuery<TWorkflowModel>
    ): Page<Workflow<TWorkflowModel>>

    /**
     * Returns the workflow with the given identifier.
     * @param identifier the identifier of the workflow to be returned
     * @param TWorkflowModel the type of the workflow's model
     * @return the workflow with the given identifier
     */
    fun <TWorkflowModel> get(identifier: WorkflowIdentifier): Workflow<TWorkflowModel>
}