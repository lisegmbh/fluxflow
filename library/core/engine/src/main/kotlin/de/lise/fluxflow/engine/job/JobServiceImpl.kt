package de.lise.fluxflow.engine.job

import de.lise.fluxflow.api.job.*
import de.lise.fluxflow.api.job.continuation.JobContinuation
import de.lise.fluxflow.api.job.query.JobQuery
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.api.workflow.WorkflowService
import de.lise.fluxflow.api.workflow.query.WorkflowQuery
import de.lise.fluxflow.api.workflow.query.filter.WorkflowFilter
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.job.query.toDataQuery
import de.lise.fluxflow.query.filter.Filter
import de.lise.fluxflow.query.pagination.Page
import de.lise.fluxflow.scheduling.SchedulingReference
import de.lise.fluxflow.scheduling.SchedulingService

class JobServiceImpl(
    private val jobActivationService: JobActivationService,
    private val jobPersistence: JobPersistence,
    private val schedulingService: SchedulingService,
    private val workflowService: WorkflowService,
) : JobService {

    override fun <TWorkflowModel, TJobModel> schedule(
        workflow: Workflow<TWorkflowModel>,
        jobContinuation: JobContinuation<TJobModel>
    ): Job {
        val model = jobContinuation.model!!
        val jobDefinition = jobActivationService.toJobDefinition(model)
        val job = jobDefinition.createJob(
            JobIdentifier(jobPersistence.randomId()),
            workflow,
            jobContinuation.scheduledTime,
            jobContinuation.cancellationKey,
            JobStatus.Scheduled,
        )

        jobContinuation.cancellationKey?.let {
            cancelAll(workflow, it)
        }

        val persistedJobData = jobPersistence.create(
            JobData(
                job.identifier.value,
                workflow.identifier.value,
                jobDefinition.kind.value,
                fetchParameters(job),
                jobContinuation.scheduledTime,
                jobContinuation.cancellationKey?.value,
                job.status,
            )
        )

        schedulingService.schedule(
            jobContinuation.scheduledTime,
            SchedulingReference(
                workflow.identifier,
                JobIdentifier(persistedJobData.id),
                jobContinuation.cancellationKey,
                job,
            )
        )
        
        return job
    }

    override fun <TWorkflowModel> cancelAll(
        workflow: Workflow<TWorkflowModel>,
        cancellationKey: CancellationKey
    ) {
        schedulingService.cancel(
            workflow.identifier,
            cancellationKey
        )
        jobPersistence.cancelJobs(
            workflow.identifier,
            cancellationKey
        )
    }

    fun setStatus(job: Job, status: JobStatus): Job {
        val newJobData = jobPersistence.findForWorkflowAndId(
            job.workflow.identifier,
            job.identifier
        )!!.withStatus(status)

        val updatedJobData = jobPersistence.save(newJobData)

        return jobActivationService.activate(job.workflow, updatedJobData)
    }

    override fun <TWorkflowModel> findJob(workflow: Workflow<TWorkflowModel>, jobIdentifier: JobIdentifier): Job? {
        return jobPersistence.findForWorkflowAndId(workflow.identifier, jobIdentifier)
            ?.let { jobActivationService.activate(workflow, it) }
    }

    override fun <TWorkflowModel> findAllJobs(workflow: Workflow<TWorkflowModel>): List<Job> {
        return jobPersistence.findForWorkflow(workflow.identifier)
            .map { jobActivationService.activate(workflow, it) }
    }

    override fun findAll(query: JobQuery): Page<Job> {
        val jobPage = jobPersistence.findAll(query.toDataQuery())
        val workflowIds = jobPage.items.map { it.workflowId }.toSet()

        val workflows = workflowService.getAll(
            WorkflowQuery.withFilter(
                WorkflowFilter.empty<Any>().withIdFilter(
                    Filter.anyOf(workflowIds)
                )
            )
        )

        return jobPage.map { job ->
            val workflow = workflows.items.single { workflow -> workflow.identifier.value == job.workflowId }

            jobActivationService.activate(
                workflow,
                job
            )
        }
    }

    override fun deleteAll(jobsIdentifiers: Set<JobIdentifier>) {
        jobPersistence.deleteAll(jobsIdentifiers)
    }

    fun deleteAllForWorkflow(workflowIdentifier: WorkflowIdentifier) {
        jobPersistence.deleteAllForWorkflow(workflowIdentifier)
    }

    private fun fetchParameters(job: Job): Map<String, Any?> {
        return job.parameters
            .associate { it.definition.kind.value to it.get() }
    }
}