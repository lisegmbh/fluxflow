package de.lise.fluxflow.persistence.job

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.job.query.JobDataQuery
import de.lise.fluxflow.query.pagination.Page

interface JobPersistence {
    /**
     * Generates a random id to be used for new objects.
     */
    fun randomId(): String

    /**
     * Persists a newly created job.
     *
     * @param jobData Information on the created job.
     * @return The persisted entity.
     */
    fun create(jobData: JobData): JobData

    /**
     * Returns all jobs associated with the given workflow.
     */
    fun findForWorkflow(workflowIdentifier: WorkflowIdentifier): List<JobData>

    /**
     * Returns all jobs that match the given query.
     * @param query The query to be used.
     * @return The found jobs.
     */
    fun findAll(query: JobDataQuery): Page<JobData>

    /**
     * Updates all jobs belonging to the workflow with the given
     * [workflowIdentifier] that have the provided [cancellationKey].
     * If the affected jobs are in a terminal state, they should not be modified.
     * @param workflowIdentifier The owning workflow's identifier.
     * @param cancellationKey The cancellation key.
     */
    fun cancelJobs(workflowIdentifier: WorkflowIdentifier, cancellationKey: CancellationKey)

    /**
     * Returns the data for the job with the given identifiers.
     *
     * @param workflowIdentifier The associated workflow's identifier.
     * @param jobIdentifier The job's identifier.
     * @return the found data or `null`.
     */
    fun findForWorkflowAndId(
        workflowIdentifier: WorkflowIdentifier,
        jobIdentifier: JobIdentifier
    ): JobData?

    /**
     * Saves all changes that occurred to the given job data.
     * @param jobData The job data to be persisted.
     * @return The updated entity.
     */
    fun save(jobData: JobData): JobData

    fun deleteAllForWorkflow(workflowIdentifier: WorkflowIdentifier)
}