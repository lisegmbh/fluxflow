package de.lise.fluxflow.mongo.job

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Projections.include
import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.persistence.job.JobData
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.job.ScheduledJobReference
import de.lise.fluxflow.persistence.job.ScheduledJobReferencePersistence
import de.lise.fluxflow.persistence.job.query.JobDataQuery
import de.lise.fluxflow.query.pagination.Page
import org.bson.Document
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

class JobMongoPersistence(
    private val jobRepository: JobRepository,
    private val queryableRepository: MongoFlowQueryRepository<JobDocument>,
    private val queryMapper: QueryMapper<JobData, JobDocument>,
    private val mongoTemplate: MongoTemplate,
) : JobPersistence, ScheduledJobReferencePersistence {
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

    override fun findScheduledJobReferences(): List<ScheduledJobReference> {
        val collection = mongoTemplate.getCollection(
            mongoTemplate.getCollectionName(JobDocument::class.java),
        )

        return collection
            .find(eq(JobDocument::jobStatus.name, JobStatus.Scheduled.name))
            .projection(include("_id", JobDocument::workflowId.name))
            .iterator()
            .use { documents ->
                documents.asSequence()
                    .mapNotNull { it.toScheduledJobReference() }
                    .toList()
            }
    }

    private fun Document.toScheduledJobReference(): ScheduledJobReference? {
        val jobId = when (val persistedId = this["_id"]) {
            is String -> persistedId
            is ObjectId -> persistedId.toHexString()
            else -> return malformedReference(
                "<unavailable>",
                "The persisted job identifier must be a String or ObjectId.",
            )
        }
        val workflowId = this[JobDocument::workflowId.name] as? String
            ?: return malformedReference(
                jobId,
                "The persisted workflow identifier must be a String.",
            )

        return ScheduledJobReference(
            WorkflowIdentifier(workflowId),
            JobIdentifier(jobId),
        )
    }

    private fun malformedReference(jobId: String, reason: String): ScheduledJobReference? {
        Logger.error(
            "Skipping malformed scheduled job reference with identifier \"{}\".",
            jobId,
            IllegalArgumentException(reason),
        )
        return null
    }

    override fun findAll(query: JobDataQuery): Page<JobData> {
        return jobRepository.findAll(
            JobDocumentQuery(query),
        ).map { it.toJobData() }
    }

    override fun findAll(query: FlowQuery<JobData, JobData>): Page<JobData> {
        return queryableRepository.find(
            queryMapper.map(query)
        ).map { 
            it.toJobData()
        }
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

    override fun deleteAll(jobsIdentifiers: Set<JobIdentifier>) {
        jobRepository.deleteAllByIdIsIn(jobsIdentifiers.map { it.value })
    }

    companion object {
        private val Logger = LoggerFactory.getLogger(JobMongoPersistence::class.java)
    }
}
