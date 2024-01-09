package de.lise.fluxflow.mongo.continuation.history.query

import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.continuation.history.query.filter.ContinuationRecordDocumentFilter
import de.lise.fluxflow.mongo.query.QueryableRepositoryImpl
import org.springframework.data.mongodb.core.MongoTemplate

class QueryableContinuationRecordRepositoryImpl(
    mongoTemplate: MongoTemplate
) : QueryableRepositoryImpl<ContinuationRecordDocument, ContinuationRecordDocumentFilter>(
    mongoTemplate,
    ContinuationRecordDocument::class
), QueryableContinuationRecordRepository