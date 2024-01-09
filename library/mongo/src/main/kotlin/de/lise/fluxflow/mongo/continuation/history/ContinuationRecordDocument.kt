package de.lise.fluxflow.mongo.continuation.history

import de.lise.fluxflow.api.WorkflowObjectReference
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class ContinuationRecordDocument(
    @Id val id: String,
    val workflowId: String,
    val timeOfOccurrence: Instant,
    val type: ContinuationType,
    val originatingObject: WorkflowObjectReference?,
    val targetObject: WorkflowObjectReference?,
) {
    
    constructor(
        id: String,
        recordData: ContinuationRecordData
    ): this(
        id,
        recordData.workflowId,
        recordData.timeOfOccurrence,
        recordData.type,
        recordData.originatingObject,
        recordData.targetObject
    )
    
    fun toRecordData(): ContinuationRecordData {
        return ContinuationRecordData(
            id,
            workflowId,
            timeOfOccurrence,
            type,
            originatingObject,
            targetObject
        )
    }
}