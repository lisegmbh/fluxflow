package de.lise.fluxflow.persistence

import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.continuation.history.ContinuationRecordIdentifier
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobKind
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.workflow.WorkflowIdentifier

class CommonToDataReplacer : PriorityExpressionReplacer(
    listOf(
        ExpressionReplacer.domainValue(WorkflowIdentifier::value),
        ExpressionReplacer.domainValue(StepIdentifier::value),
        ExpressionReplacer.domainValue(StepKind::value),
        ExpressionReplacer.domainValue(JobKind::value),
        ExpressionReplacer.domainValue(JobIdentifier::value),
        ExpressionReplacer.domainValue(ContinuationRecordIdentifier::value)
    )
)