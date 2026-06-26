package de.lise.fluxflow.persistence.workflow.flowquery

import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.workflow.flowquery.WorkflowQueryable
import de.lise.fluxflow.persistence.CommonToDataReplacer
import de.lise.fluxflow.persistence.workflow.WorkflowData
import kotlin.reflect.typeOf

class WorkflowQueryableToDataReplacer : PriorityExpressionReplacer(
    listOf(
        ExpressionReplacer.root(typeOf<WorkflowData>()) {
            typeOf<WorkflowQueryable<*>>().classifier == it.classifier
        },
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