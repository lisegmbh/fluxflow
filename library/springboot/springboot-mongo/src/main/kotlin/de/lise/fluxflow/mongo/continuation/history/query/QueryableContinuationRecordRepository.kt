package de.lise.fluxflow.mongo.continuation.history.query

import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.mongo.continuation.history.query.filter.ContinuationRecordDocumentFilter
import de.lise.fluxflow.mongo.query.QueryableRepository

interface QueryableContinuationRecordRepository : QueryableRepository<
        ContinuationRecordDocument,
        ContinuationRecordDocumentFilter
>

