package de.lise.fluxflow.api.job

import de.lise.fluxflow.api.job.continuation.JobContinuation
import de.lise.fluxflow.api.job.query.JobQuery
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.query.pagination.Page

interface JobService {
    /**
     * Schedules the given job continuation.
     * @param workflow The owning workflow for which the job should be scheduled.
     * @param jobContinuation The [JobContinuation] to be scheduled.
     */
    fun <TWorkflowModel, TJobModel> schedule(
        workflow: Workflow<TWorkflowModel>,
        jobContinuation: JobContinuation<TJobModel>
    ) : Job

    fun <TWorkflowModel> cancelAll(
        workflow: Workflow<TWorkflowModel>,
        cancellationKey: CancellationKey
    )

    fun <TWorkflowModel> findJob(
        workflow: Workflow<TWorkflowModel>,
        jobIdentifier: JobIdentifier
    ): Job?

    fun <TWorkflowModel> findAllJobs(
        workflow: Workflow<TWorkflowModel>
    ): List<Job>

    fun findAll(query: JobQuery): Page<Job>
}