package de.lise.fluxflow.persistence.continuation.history

import de.lise.fluxflow.persistence.continuation.history.query.ContinuationRecordDataQuery
import de.lise.fluxflow.query.pagination.Page

interface ContinuationRecordPersistence {
    /**
     * Persists a new continuation record.
     * Implementations must assign a new id to [ContinuationRecordData.id].
     * @return The persisted [continuationRecord], that's [ContinuationRecordData.id] has been assigned.
     * @throws IllegalArgumentException Is thrown, if the provided [continuationRecord] has a non `null` [ContinuationRecordData.id].
     */
    fun create(continuationRecord: ContinuationRecordData): ContinuationRecordData
    fun findAll(query: ContinuationRecordDataQuery): Page<ContinuationRecordData>
}