package de.lise.fluxflow.persistence.workflow.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.DefaultToDataMapper
import de.lise.fluxflow.persistence.workflow.WorkflowData

class WorkflowToDataMapper : ExpressionMapper {
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.property(Workflow<*>::model) {
                PropertyExpression(
                    Expression.root(),
                    WorkflowData::model
                )
            },
            ExpressionReplacer.property(Workflow<*>::identifier) {
                PropertyExpression(
                    Expression.root(),
                    WorkflowData::id
                )
            },
            DefaultToDataMapper(),
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(
            expression
        )
    }
}