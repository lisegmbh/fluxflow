package de.lise.fluxflow.api.job

import de.lise.fluxflow.api.job.continuation.JobContinuation
import de.lise.fluxflow.api.job.query.JobQuery
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.query.pagination.Page

/**
 * Service interface for interacting with jobs in FluxFlow.
 *
 * Jobs represent asynchronous units of work that are scheduled to run at a specific time.
 * This service provides operations to schedule, reschedule, cancel, duplicate, retrieve,
 * and delete jobs associated with a workflow.
 */
interface JobService {
    /**
     * Schedules a new job for the given [workflow] using the provided [jobContinuation].
     *
     * The job will be persisted and executed at the time specified by the continuation.
     * If a cancellation key is provided within the continuation and another job with the same key already exists,
     * it will be replaced.
     *
     * @param workflow The workflow that owns the scheduled job.
     * @param jobContinuation The job continuation to be scheduled.
     * @return The resulting scheduled [Job] instance.
     */
    fun <TWorkflowModel, TJobModel> schedule(
        workflow: Workflow<TWorkflowModel>,
        jobContinuation: JobContinuation<TJobModel>
    ) : Job

    /**
     * Duplicates a given [job] and reschedules it for the provided [workflow].
     *
     * This is useful when jobs need to be retried or manually repeated.
     *
     * @param workflow The workflow in which to reschedule the duplicated job.
     * @param job The job instance to be duplicated and rescheduled.
     * @return The newly scheduled duplicate job.
     */
    fun <TWorkflowModel> duplicateJob(
        workflow: Workflow<TWorkflowModel>,
        job: Job
    ) : Job

    /**
     * Cancels all jobs associated with the given [workflow] and [cancellationKey].
     *
     * Jobs can only be canceled if they are still in the `Scheduled` state.
     *
     * @param workflow The workflow whose jobs should be canceled.
     * @param cancellationKey The key identifying which jobs to cancel.
     */
    fun <TWorkflowModel> cancelAll(
        workflow: Workflow<TWorkflowModel>,
        cancellationKey: CancellationKey
    )

    /**
     * Deprecated: Use [getJob] or [getJobOrNull] instead.
     *
     * @see getJobOrNull
     */
    @Deprecated(
        "In order to align the API naming, this function will be replace by .getJob() or .getJobOrNull()",
            replaceWith = ReplaceWith("getJobOrNull<TWorkflowModel>(workflow, jobIdentifier)")
    )
    fun <TWorkflowModel> findJob(
        workflow: Workflow<TWorkflowModel>,
        jobIdentifier: JobIdentifier
    ): Job?

    /**
     * Retrieves the job with the given [jobIdentifier] for the specified [workflow].
     *
     * Throws an exception if the job does not exist.
     *
     * @param workflow The workflow that owns the job.
     * @param jobIdentifier The identifier of the job to retrieve.
     * @return The [Job] instance.
     */
    fun <TWorkflowModel> getJob(
        workflow: Workflow<TWorkflowModel>,
        jobIdentifier: JobIdentifier
    ): Job

    /**
     * Retrieves the job with the given [jobIdentifier] for the specified [workflow], or `null` if it does not exist.
     *
     * @param workflow The workflow that owns the job.
     * @param jobIdentifier The identifier of the job to retrieve.
     * @return The [Job] instance, or `null` if not found.
     */
    fun <TWorkflowModel> getJobOrNull(
        workflow: Workflow<TWorkflowModel>,
        jobIdentifier: JobIdentifier
    ): Job?

    /**
     * Returns all jobs currently associated with the given [workflow].
     *
     * This includes scheduled, completed, and possibly failed jobs depending on the engine configuration.
     *
     * @param workflow The workflow whose jobs should be returned.
     * @return A list of [Job] instances.
     */
    fun <TWorkflowModel> findAllJobs(
        workflow: Workflow<TWorkflowModel>
    ): List<Job>

    /**
     * Returns a paginated list of jobs that match the given [query].
     *
     * This allows for filtering jobs globally (e.g., across all workflows) based on criteria
     * such as status, job type, or creation time.
     *
     * @param query The query object defining search criteria.
     * @return A [Page] containing matching [Job] instances.
     */
    fun findAll(query: JobQuery): Page<Job>

    /**
     * Deletes all jobs matching the given [jobsIdentifiers].
     *
     * This operation should be used with caution, especially in production environments,
     * as it may interfere with workflow execution or state consistency.
     *
     * @param jobsIdentifiers The identifiers of the jobs to delete.
     */
    fun deleteAll(jobsIdentifiers: Set<JobIdentifier>)
}