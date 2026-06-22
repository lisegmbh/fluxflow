package de.lise.fluxflow.springboot.rest.workflow.step

import de.fluxflow.flowquery.expression.Expression
import de.fluxflow.flowquery.expression.PropertyExpression
import de.fluxflow.flowquery.mapper.expression.ExpressionReplacer
import de.fluxflow.flowquery.mapper.expression.PriorityExpressionReplacer
import de.lise.fluxflow.api.step.StepIdentifier
import de.lise.fluxflow.api.step.StepKind
import de.lise.fluxflow.api.step.query.StepQueryable
import de.lise.fluxflow.api.workflow.WorkflowIdentifier
import de.lise.fluxflow.rest.workflow.step.StatefulStepSpecDto
import de.lise.fluxflow.rest.workflow.step.StepMetadataDto
import de.lise.fluxflow.rest.workflow.step.StepStatusDto
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.typeOf

class StepDtoToStepQueryableReplacer : ExpressionReplacer {
    private val replacer = PriorityExpressionReplacer(
        listOf(
            ExpressionReplacer {
                when (it) {
                    is PropertyExpression<*, *, *> -> when (it.property.instanceParameter?.type) {
                        typeOf<StepMetadataDto>() -> when (it.property) {
                            StepMetadataDto::kind -> Expression.root<StepQueryable>()
                                .get(StepQueryable::kind)
                                .get(StepKind::value)

                            StepMetadataDto::stepIdentifier -> Expression.root<StepQueryable>()
                                .get(StepQueryable::identifier)
                                .get(StepIdentifier::value)

                            StepMetadataDto::workflowIdentifier -> Expression.root<StepQueryable>()
                                .get(StepQueryable::workflowIdentifier)
                                .get(WorkflowIdentifier::value)

                            StepMetadataDto::annotations -> Expression.root<StepQueryable>()
                                .get(StepQueryable::metadata)

                            else -> null
                        }

                        typeOf<StatefulStepSpecDto>() -> when (it.property) {
                            StatefulStepSpecDto::data -> Expression.root<StepQueryable>()
                                .get(StepQueryable::data)

                            else -> null
                        }

                        typeOf<StepStatusDto>() -> when (it.property) {
                            StepStatusDto::status -> Expression.root<StepQueryable>()
                                .get(StepQueryable::status)

                            else -> null
                        }

                        else -> null
                    }

                    else -> null
                }
            }
        )
    )

    override fun replace(expression: Expression<*, *>): Expression<*, *>? {
        return replacer.replace(expression)
    }

}

