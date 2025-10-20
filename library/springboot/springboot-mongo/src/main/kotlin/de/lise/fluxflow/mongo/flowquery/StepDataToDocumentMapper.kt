package de.lise.fluxflow.mongo.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.persistence.step.StepData

class StepDataToDocumentMapper : ExpressionMapper {
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.Companion.property(
                StepData::id,
                StepDocument::id
            ),
            ExpressionReplacer.Companion.property(
                StepData::workflowId,
                StepDocument::workflowId
            ),
            ExpressionReplacer.Companion.property(
                StepData::kind,
                StepDocument::kind,
            ),
            ExpressionReplacer.Companion.property(
                StepData::version,
                StepDocument::version
            ),
            ExpressionReplacer.Companion.property(StepData::data) {
                val typedRecords = PropertyExpression(
                    it.instance as Expression<Any, StepDocument>,
                    StepDocument::dataEntries
                ) as Expression<Any, TypedRecords<Any?>>
                typedRecords.get(TypedRecords<Any?>::values)
            },
            ExpressionReplacer.Companion.property(
                StepData::status,
                StepDocument::status
            ),
            ExpressionReplacer.Companion.property(StepData::metadata) {
                val typedRecords = PropertyExpression(
                    it.instance as Expression<Any, StepDocument>,
                    StepDocument::metadataEntries
                ) as Expression<Any, TypedRecords<Any>>
                typedRecords.get(TypedRecords<Any>::values)
            }
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(expression)
    }
}