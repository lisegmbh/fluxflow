package de.lise.fluxflow.persistence.step.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.step.query.StepQueryable
import de.lise.fluxflow.persistence.DefaultToDataMapper
import de.lise.fluxflow.persistence.step.StepData

class StepQueryableToDataMapper : ExpressionMapper {

    private val mapper = PriorityExpressionReplacer(
        listOf(
            DefaultToDataMapper(),
            ExpressionReplacer.property(StepQueryable::identifier) {
                PropertyExpression(
                    it.instance as Expression<StepData, StepData>,
                    StepData::id
                )
            },
            ExpressionReplacer.property(StepQueryable::workflowIdentifier) {
                PropertyExpression(
                    it.instance as Expression<Any, StepData>,
                    StepData::workflowId
                )
            },
            ExpressionReplacer.property(StepQueryable::kind) {
                PropertyExpression(
                    it.instance as Expression<Any, StepData>,
                    StepData::kind
                )
            },
            ExpressionReplacer.property(StepQueryable::status) {
                PropertyExpression(
                    it.instance as Expression<Any, StepData>,
                    StepData::status
                )
            },
            ExpressionReplacer.property(StepQueryable::data) {
                PropertyExpression(
                    it.instance as Expression<Any, StepData>,
                    StepData::data
                )
            },
            ExpressionReplacer.property(StepQueryable::metadata) {
                PropertyExpression(
                    it.instance as Expression<Any, StepData>,
                    StepData::metadata
                )
            }
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(expression)
    }
}