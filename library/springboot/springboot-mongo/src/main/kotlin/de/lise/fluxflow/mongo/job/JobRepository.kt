package de.lise.fluxflow.mongo.job

import de.lise.fluxflow.api.job.JobStatus
import de.lise.fluxflow.mongo.job.query.QueryableJobRepository
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Update

interface JobRepository : MongoRepository<JobDocument, String>, QueryableJobRepository {
    fun findByWorkflowId(workflowId: String): List<JobDocument>
    fun findByIdAndWorkflowId(id: String, workflowId: String): JobDocument?
    @Update("{ '\$set': { 'jobStatus': 'Canceled' } }")
    fun findAndSetJobStatusByWorkflowIdAndCancellationKeyAndJobStatusIn(
        workflowId: String,
        cancellationKey: String,
        status: Set<JobStatus>
    )
    fun deleteAllByWorkflowId(workflowId: String)
    fun deleteAllByIdIsIn(ids: List<String>)
}