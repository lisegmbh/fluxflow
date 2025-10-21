package de.lise.fluxflow.persistence.workflow.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.persistence.CommonToDataMapper
import de.lise.fluxflow.persistence.workflow.WorkflowData

class WorkflowToDataMapper : ExpressionMapper {
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.property(
                Workflow<*>::identifier,
                WorkflowData::id
            ),
            ExpressionReplacer.property(
                Workflow<*>::model,
                WorkflowData::model
            ),
            CommonToDataMapper(),
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(
            expression
        )
    }
}