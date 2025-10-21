package de.lise.fluxflow.persistence.job.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.job.query.JobQueryable
import de.lise.fluxflow.persistence.DefaultToDataMapper
import de.lise.fluxflow.persistence.job.JobData

class JobQueryableToDataMapper : ExpressionMapper {
    
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.property(
                JobQueryable::identifier,
                JobData::id
            ),
            ExpressionReplacer.property(
                JobQueryable::workflowIdentifier,
                JobData::workflowId
            ),
            ExpressionReplacer.property(
                JobQueryable::kind,
                JobData::kind
            ),
            ExpressionReplacer.property(
                JobQueryable::parameters,
                JobData::parameters
            ),
            ExpressionReplacer.property(
                JobQueryable::scheduledTime,
                JobData::scheduledTime
            ),
            ExpressionReplacer.property(
                JobQueryable::cancellationKey,
                JobData::cancellationKey
            ),
            ExpressionReplacer.property(
                JobQueryable::status,
                JobData::status
            ),
            DefaultToDataMapper()
        )
    )
    
    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(expression)
    }
}