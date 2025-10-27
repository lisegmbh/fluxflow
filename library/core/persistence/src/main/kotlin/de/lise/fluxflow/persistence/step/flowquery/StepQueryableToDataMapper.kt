package de.lise.fluxflow.persistence.step.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.step.query.StepQueryable
import de.lise.fluxflow.persistence.CommonToDataMapper
import de.lise.fluxflow.persistence.step.StepData

class StepQueryableToDataMapper : ExpressionMapper {

    private val mapper = PriorityExpressionReplacer(
        listOf(
            CommonToDataMapper(),
            ExpressionReplacer.property(
                StepQueryable::identifier,
                StepData::id
            ),
            ExpressionReplacer.property(
                StepQueryable::workflowIdentifier,
                StepData::workflowId    
            ),
            ExpressionReplacer.property(
                StepQueryable::kind,
                StepData::kind
            ),
            ExpressionReplacer.property(
                StepQueryable::status,
                StepData::status
            ),
            ExpressionReplacer.property(
                StepQueryable::data,
                StepData::data
            ),
            ExpressionReplacer.property(
                StepQueryable::metadata,
                StepData::metadata
            ) 
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(expression)
    }
}