package de.lise.fluxflow.mongo.workflow.flowquery

import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.mongo.workflow.WorkflowDocument
import de.lise.fluxflow.persistence.workflow.WorkflowData

class WorkflowDataToDocumentReplacer : PriorityExpressionReplacer(
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