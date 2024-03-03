package de.lise.fluxflow.persistence.step

import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.step.query.StepDataQuery
import de.lise.fluxflow.query.pagination.Page

interface StepPersistence {
    /**
     * Generates a random id to be used for new objects.
     */
    fun randomId(): String

    /**
     * Persists a newly created workflow step.
     *
     * @param stepData Information on the started workflow step.
     * @return The persisted entity.
     */
    fun create(stepData: StepData): StepData

    /**
     * Returns all steps associated to the given workflow.
     */
    fun findForWorkflow(workflowIdentifier: WorkflowIdentifier): List<StepData>

    /**
     * Returns all steps associated to the given workflow and fulfilling the query.
     *
     * **Important**:
     * It is the implementation's job to do the necessary pre-filtering for the requested [workflowIdentifier].
     * It should not be expected to be part of the regular [query].
     */
    fun findForWorkflow(workflowIdentifier: WorkflowIdentifier, query: StepDataQuery): Page<StepData>

    fun findForWorkflowAndId(workflowIdentifier: WorkflowIdentifier, stepIdentifier: StepIdentifier): StepData?
    fun save(stepData: StepData): StepData
    fun deleteMany(stepIdentifiers: Set<StepIdentifier>)
}