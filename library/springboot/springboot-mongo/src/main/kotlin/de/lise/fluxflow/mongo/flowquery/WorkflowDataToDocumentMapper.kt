package de.lise.fluxflow.mongo.flowquery

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.workflow.WorkflowData


class WorkflowDataToDocumentMapper : ExpressionMapper {
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.property(
                WorkflowData::id,
                WorkflowDocument::id
            ),
            ExpressionReplacer.property(
                WorkflowData::model,
                WorkflowDocument::model
            ),
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(
            expression
        )
    }
}

