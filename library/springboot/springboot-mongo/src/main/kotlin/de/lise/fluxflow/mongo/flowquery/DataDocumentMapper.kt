package de.lise.fluxflow.mongo.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.workflow.WorkflowData

class DataDocumentMapper : ExpressionMapper {
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.property(
                WorkflowData::id
            ) {
                PropertyExpression(
                    it.instance as Expression<Any, WorkflowDocument>,
                    WorkflowDocument::id
                )
            },
            ExpressionReplacer.property(
                WorkflowData::model
            ) {
                PropertyExpression(
                    it.instance as Expression<Any, WorkflowDocument>,
                    WorkflowDocument::model
                )
            }
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *> {
        return mapper.replace(
            expression
        )
    }
}