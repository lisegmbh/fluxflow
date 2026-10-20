package de.lise.fluxflow.persistence.continuation.history.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.continuation.history.query.ContinuationRecordQueryable
import de.lise.fluxflow.persistence.CommonToDataReplacer
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordData

class ContinuationRecordQueryableToDataMapper : ExpressionMapper {
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.property(
                ContinuationRecordQueryable::id,
                ContinuationRecordData::id
            ),
            ExpressionReplacer.property(
                ContinuationRecordQueryable::workflowIdentifier,
                ContinuationRecordData::workflowId
            ),
            ExpressionReplacer.property(
                ContinuationRecordQueryable::timeOfOccurrence,
                ContinuationRecordData::timeOfOccurrence
            ),
            ExpressionReplacer.property(
                ContinuationRecordQueryable::type,
                ContinuationRecordData::type
            ),
            ExpressionReplacer.property(
                ContinuationRecordQueryable::originatingObject,
                ContinuationRecordData::originatingObject
            ),
            ExpressionReplacer.property(
                ContinuationRecordQueryable::targetObject,
                ContinuationRecordData::targetObject
            ),
            CommonToDataReplacer()
        )
    ).toMapper()

    override fun map(expression: Expression<*, *>): Expression<*, *> {
        return mapper.map(expression)
    }
}