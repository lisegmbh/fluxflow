package de.lise.fluxflow.api.workflow

import de.fluxflow.flowquery.expression.ExpressionExtensions.Types.isType
import de.fluxflow.flowquery.query.FlowQuery
import de.fluxflow.flowquery.query.FlowQueryBuilder
import de.fluxflow.flowquery.service.ResourceQueryService
import de.lise.fluxflow.api.workflow.flowquery.WorkflowQueryable
import de.lise.fluxflow.api.workflow.flowquery.WorkflowQueryable.Companion.model
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
interface WorkflowQueryService : ResourceQueryService<Workflow<*>, WorkflowQueryable<*>> {
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
    @Deprecated("Use the new FlowQuery overloads instead.")
    fun getAll(query: WorkflowQuery<*>): Page<Workflow<*>>

    /**
     * Returns all workflows matching the given query.
     * @param modelType the type of the workflows' model
     * @param query the query that should be applied to filter the workflows
     * @param TWorkflowModel the type of the workflows' model
     * @return a page of workflows matching the given query
     */
    @Deprecated("Use the new FlowQuery overloads instead.")
    fun <TWorkflowModel : Any> getAll(
        modelType: KClass<TWorkflowModel>,
        query: WorkflowQuery<TWorkflowModel>
    ): Page<Workflow<TWorkflowModel>>

    /**
     * Returns the workflow with the given identifier.
     * @param identifier the identifier of the workflow to be returned
     * @param TWorkflowModel the type of the workflow's model
     * @return the workflow with the given identifier
     * @throws WorkflowNotFoundException if there is no workflow with the given [identifier].
     */
    fun <TWorkflowModel> get(identifier: WorkflowIdentifier): Workflow<TWorkflowModel>

    /**
     * Returns the workflow with the given identifier.
     * @param identifier the identifier of the workflow to be returned
     * @param TWorkflowModel the type of the workflow's model
     * @return the workflow with the given identifier, or `null` if there is no workflow with the given [identifier].
     */
    fun <TWorkflowModel> getOrNull(identifier: WorkflowIdentifier): Workflow<TWorkflowModel>?

    /**
     * Returns a page of workflows whose model type matches [type] and that satisfy the constraints
     * defined in [query]. Callers do not need to manually filter by model type or cast; the resulting
     * page contains only workflows with a model of the requested type.
     *
     * The supplied [query] may include filters, sorting and pagination; these are applied after the
     * automatic type filtering. If no workflows match, an empty page is returned.
     *
     * @param type the workflow model class to select
     * @param query the flow query specifying additional filters, sorting and pagination
     * @param TWorkflowModel the workflow model type
     * @return a page containing only workflows whose model is of type [TWorkflowModel]
     */
    @Suppress("UNCHECKED_CAST")
    fun <TWorkflowModel : Any> findAll(
        type: KClass<TWorkflowModel>,
        query: FlowQuery<WorkflowQueryable<TWorkflowModel>, WorkflowQueryable<TWorkflowModel>>
    ): Page<Workflow<TWorkflowModel>> {
        val typedQuery = FlowQuery.of<WorkflowQueryable<Any>>().where {
            model.isType(type) // This asserts the model type, so we can safely cast the workflow data model
        } as FlowQuery<WorkflowQueryable<TWorkflowModel>, WorkflowQueryable<TWorkflowModel>>
        
        val combinedQuery = typedQuery.append(
            query
        )
        
        return findAll(combinedQuery as FlowQuery<WorkflowQueryable<*>, WorkflowQueryable<*>>).map {
            it as Workflow<TWorkflowModel>
        }
    }
    
    /**
     * Returns a page of workflows of the given model [type] using a query built by [queryBuilder].
     * Implementations automatically restrict results to the specified model type, so callers can focus
     * solely on defining domain filters, sorting or pagination inside the builder.
     *
     * If the built query matches
     * no workflows, an empty page is returned.
     *
     * @param type the workflow model class to select
     * @param queryBuilder lambda receiving a fresh query to configure and returning the built query
     * @param TWorkflowModel the workflow model type
     * @return a page containing only workflows whose model is of type [TWorkflowModel]
     */
    fun <TWorkflowModel: Any> findAll(
        type: KClass<TWorkflowModel>,
        queryBuilder: FlowQueryBuilder<WorkflowQueryable<TWorkflowModel>, WorkflowQueryable<TWorkflowModel>>
    ): Page<Workflow<TWorkflowModel>> {
        return findAll(
            type,
            queryBuilder(
                FlowQuery.of()
            )
        )
    }
}
