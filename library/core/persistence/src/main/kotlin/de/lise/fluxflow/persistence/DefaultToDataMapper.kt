package de.lise.fluxflow.persistence

import de.fluxflow.flowquery.expression.Constant
import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.mapper.expression.ExpressionMapper
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.job.JobIdentifier
import de.lise.fluxflow.api.job.JobKind
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.workflow.WorkflowIdentifier

class DefaultToDataMapper : ExpressionMapper {
    private val mapper = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer.constantOfType<WorkflowIdentifier> {
                Constant<Any, String>(
                    it.value
                )
            },
            ExpressionReplacer.property(WorkflowIdentifier::value) {
                it.instance
            },

            ExpressionReplacer.constantOfType<StepIdentifier> {
                Constant<Any, String>(
                    it.value
                )
            },
            ExpressionReplacer.property(StepIdentifier::value) {
                it.instance
            },

            ExpressionReplacer.constantOfType<StepKind> {
                Constant<Any, String>(
                    it.value
                )
            },
            ExpressionReplacer.property(StepKind::value) {
                it.instance
            },

            ExpressionReplacer.constantOfType<JobKind> { 
                Constant<Any, String>(
                    it.value
                )
            },
            ExpressionReplacer.property(JobKind::value) {
                it.instance
            },

            ExpressionReplacer.constantOfType<JobIdentifier> {
                Constant<Any, String>(
                    it.value
                )
            },
            ExpressionReplacer.property(JobIdentifier::value) {
                it.instance
            }
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return mapper.replace(
            expression
        )
    }
}