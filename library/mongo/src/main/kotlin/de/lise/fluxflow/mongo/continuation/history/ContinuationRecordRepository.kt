package de.lise.fluxflow.mongo.continuation.history

import de.lise.fluxflow.mongo.continuation.history.query.QueryableContinuationRecordRepository
import org.springframework.data.mongodb.repository.MongoRepository

interface ContinuationRecordRepository : MongoRepository<ContinuationRecordDocument, String>, QueryableContinuationRecordRepository