package de.lise.fluxflow.mongo.continuation.history

import de.lise.fluxflow.mongo.continuation.history.query.QueryableContinuationRecordRepository
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface ContinuationRecordRepository : MongoRepository<ContinuationRecordDocument, String>, QueryableContinuationRecordRepository {
    fun deleteAllByWorkflowId(workflowId: String)
}
