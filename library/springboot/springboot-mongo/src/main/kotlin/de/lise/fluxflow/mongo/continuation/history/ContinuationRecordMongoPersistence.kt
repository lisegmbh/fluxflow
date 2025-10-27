package de.lise.fluxflow.mongo.continuation.history

import de.fluxflow.flowquery.mapper.query.QueryMapper
import de.fluxflow.flowquery.query.FlowQuery
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.mongo.continuation.history.query.ContinuationRecordDocumentQuery
import de.lise.fluxflow.mongo.flowquery.repository.MongoFlowQueryRepository
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.continuation.history.query.ContinuationRecordDataQuery
import de.lise.fluxflow.query.pagination.Page
import org.bson.types.ObjectId

class ContinuationRecordMongoPersistence(
    private val continuationRecordRepository: ContinuationRecordRepository,
    private val queryableRepository: MongoFlowQueryRepository<ContinuationRecordDocument>,
    private val queryMapper: QueryMapper<ContinuationRecordData, ContinuationRecordDocument>,
) : ContinuationRecordPersistence {
    override fun create(continuationRecord: ContinuationRecordData): ContinuationRecordData {
        if (continuationRecord.id != null) {
            throw IllegalArgumentException("The given record data seems to be already persisted, as the id is not null")
        }
        return continuationRecordRepository.save(
            ContinuationRecordDocument(
                ObjectId.get()!!.toHexString(),
                continuationRecord,
            )
        ).toRecordData()
    }

    override fun findAll(query: ContinuationRecordDataQuery): Page<ContinuationRecordData> {
        return continuationRecordRepository.findAll(
            ContinuationRecordDocumentQuery(query)
        ).map{ it.toRecordData() }
    }

    override fun findAll(query: FlowQuery<ContinuationRecordData, ContinuationRecordData>): Page<ContinuationRecordData> {
        return queryableRepository.find(
            queryMapper.map(query)
        ).map { 
            it.toRecordData()
        }
    }

    override fun deleteAllForWorkflow(identifierToDelete: WorkflowIdentifier) {
        continuationRecordRepository.deleteAllByWorkflowId(identifierToDelete.value)
    }
}