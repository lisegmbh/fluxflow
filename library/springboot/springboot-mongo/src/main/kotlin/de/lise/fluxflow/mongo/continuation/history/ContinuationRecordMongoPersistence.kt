package de.lise.fluxflow.mongo.continuation.history

import de.lise.fluxflow.mongo.continuation.history.query.ContinuationRecordDocumentQuery
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.continuation.history.query.ContinuationRecordDataQuery
import de.lise.fluxflow.query.pagination.Page
import org.bson.types.ObjectId

class ContinuationRecordMongoPersistence(
    private val continuationRecordRepository: ContinuationRecordRepository
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
}