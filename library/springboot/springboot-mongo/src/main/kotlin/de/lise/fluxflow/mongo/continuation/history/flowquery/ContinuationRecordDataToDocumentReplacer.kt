package de.lise.fluxflow.mongo.continuation.history.flowquery

import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.mongo.continuation.history.ContinuationRecordDocument
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData

class ContinuationRecordDataToDocumentReplacer : PriorityExpressionReplacer(
    listOf(
        ExpressionReplacer.property(
            ContinuationRecordData::id,
            ContinuationRecordDocument::id
        ),
        ExpressionReplacer.property(
            ContinuationRecordData::workflowId,
            ContinuationRecordDocument::workflowId
        ),
        ExpressionReplacer.property(
            ContinuationRecordData::timeOfOccurrence,
            ContinuationRecordDocument::timeOfOccurrence
        ),
        ExpressionReplacer.property(
            ContinuationRecordData::type,
            ContinuationRecordDocument::type
        ),
        ExpressionReplacer.property(
            ContinuationRecordData::originatingObject,
            ContinuationRecordDocument::originatingObject
        ),
        ExpressionReplacer.property(
            ContinuationRecordData::targetObject,
            ContinuationRecordDocument::targetObject
        )
    )
)