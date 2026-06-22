package de.lise.fluxflow.mongo.step.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.step.StepDocument
import de.lise.fluxflow.persistence.step.StepData

class StepDataToDocumentReplacer : PriorityExpressionReplacer(
    listOf(
        ExpressionReplacer.property(
            StepData::id,
            StepDocument::id
        ),
        ExpressionReplacer.property(
            StepData::workflowId,
            StepDocument::workflowId
        ),
        ExpressionReplacer.property(
            StepData::kind,
            StepDocument::kind,
        ),
        ExpressionReplacer.property(
            StepData::version,
            StepDocument::version
        ),
        ExpressionReplacer.property(StepData::data) {
            val typedRecords = PropertyExpression(
                it.instance as Expression<Any, StepDocument>,
                StepDocument::dataEntries
            ) as Expression<Any, TypedRecords<Any?>>
            typedRecords.get(TypedRecords<Any?>::values)
        },
        ExpressionReplacer.property(
            StepData::status,
            StepDocument::status
        ),
        ExpressionReplacer.property(StepData::metadata) {
            val typedRecords = PropertyExpression(
                it.instance as Expression<Any, StepDocument>,
                StepDocument::metadataEntries
            ) as Expression<Any, TypedRecords<Any>>
            typedRecords.get(TypedRecords<Any>::values)
        }
    )
)