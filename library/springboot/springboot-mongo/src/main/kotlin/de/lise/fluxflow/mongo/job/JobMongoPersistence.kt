package de.lise.fluxflow.mongo.job

import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.job.query.JobDataQuery
import de.lise.fluxflow.query.pagination.Page
import org.bson.types.ObjectId

class JobMongoPersistence(
    private val jobRepository: JobRepository
) : JobPersistence {
    override fun randomId(): String {
        return ObjectId.get()!!.toHexString()
    }

    override fun create(jobData: JobData): JobData {
        return jobRepository.insert(
            JobDocument(
                jobData.id,
                jobData.workflowId,
                jobData.kind,
                jobData.parameters,
                jobData.scheduledTime,
                jobData.cancellationKey,
                jobData.status
            )
        ).toJobData()
    }

    override fun findForWorkflow(workflowIdentifier: WorkflowIdentifier): List<JobData> {
        return jobRepository.findByWorkflowId(workflowIdentifier.value).map {
            it.toJobData()
        }
    }

    override fun findAll(query: JobDataQuery): Page<JobData> {
        return jobRepository.findAll(
            JobDocumentQuery(query),
        ).map { it.toJobData() }
    }

    override fun cancelJobs(workflowIdentifier: WorkflowIdentifier, cancellationKey: CancellationKey) {
        jobRepository.findAndSetJobStatusByWorkflowIdAndCancellationKeyAndJobStatusIn(
            workflowIdentifier.value,
            cancellationKey.value,
            setOf(JobStatus.Scheduled)
        )
    }

    override fun findForWorkflowAndId(workflowIdentifier: WorkflowIdentifier, jobIdentifier: JobIdentifier): JobData? {
        return jobRepository.findByIdAndWorkflowId(
            jobIdentifier.value,
            workflowIdentifier.value
        )?.toJobData()
    }

    override fun save(jobData: JobData): JobData {
        return jobRepository.save(
            JobDocument(
                jobData.id,
                jobData.workflowId,
                jobData.kind,
                jobData.parameters,
                jobData.scheduledTime,
                jobData.cancellationKey,
                jobData.status
            )
        ).toJobData()
    }

    override fun deleteAllForWorkflow(workflowIdentifier: WorkflowIdentifier) {
        jobRepository.deleteAllByWorkflowId(workflowIdentifier.value)
    }
}
