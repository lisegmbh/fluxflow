package de.lise.fluxflow.persistence.workflow.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.workflow.flowquery.WorkflowQueryable
import de.lise.fluxflow.persistence.CommonToDataReplacer
import de.lise.fluxflow.persistence.workflow.WorkflowData

class WorkflowQueryableToDataReplacer : ExpressionReplacer {
    private val replacer = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.property(
                WorkflowQueryable<*>::identifier,
                WorkflowData::id
            ),
            ExpressionReplacer.property(
                WorkflowQueryable<*>::model,
                WorkflowData::model
            ),
            CommonToDataReplacer(),
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return replacer.replace(
            expression
        )
    }
}