package de.lise.fluxflow.mongo.job.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.mongo.generic.record.TypedRecords
import de.lise.fluxflow.mongo.job.JobDocument
import de.lise.fluxflow.persistence.job.JobData

class JobDataToDocumentMapper : ExpressionMapper {
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.property(
                JobData::id,
                JobDocument::id
            ),
            ExpressionReplacer.property(
                JobData::workflowId,
                JobDocument::workflowId
            ),
            ExpressionReplacer.property(
                JobData::kind,
                JobDocument::kind
            ),
            ExpressionReplacer.property(JobData::parameters) {
                val typedRecords = PropertyExpression(
                    it.instance as Expression<Any, JobDocument>,
                    JobDocument::parameterEntries
                ) as Expression<Any, TypedRecords<Any?>>
                typedRecords.get(TypedRecords<Any?>::values)
            },
            ExpressionReplacer.property(
                JobData::scheduledTime,
                JobDocument::scheduledTime
            ),
            ExpressionReplacer.property(
                JobData::cancellationKey,
                JobDocument::cancellationKey
            ),
            ExpressionReplacer.property(
                JobData::status,
                JobDocument::jobStatus
            )
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(expression)
    }
}