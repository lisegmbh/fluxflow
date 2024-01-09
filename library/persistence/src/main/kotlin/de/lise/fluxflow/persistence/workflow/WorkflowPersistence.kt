package de.lise.fluxflow.persistence.workflow

import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.workflow.query.WorkflowDataQuery
import de.lise.fluxflow.query.pagination.Page

interface WorkflowPersistence {

    /**
     * Persists the given workflow data (representing the state of a newly started workflow) and returns the
     * persisted entity.
     * It is the implementation responsibility to create a unique workflow identifier.
     *
     * @param model The workflow data representing its current state.
     * @param forcedId If non-null, the workflow will have the given identifier.
     * @return The persisted workflow entity.
     */
    fun create(model: Any?, forcedId: WorkflowIdentifier?): WorkflowData

    /**
     * Returns all persisted workflows.
     *
     * @return All currently persisted workflows.
     */
    fun findAll(): List<WorkflowData>
    fun findAll(query: WorkflowDataQuery): Page<WorkflowData>
    fun find(id: WorkflowIdentifier): WorkflowData?
    fun save(workflowData: WorkflowData): WorkflowData
    fun delete(id: WorkflowIdentifier)

}