package de.lise.fluxflow.engine.step.validation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.api.step.stateful.StatefulStep
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationException
import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

class ValidationService(
    private val validateBeforeActions: Boolean
) {
    fun validateBeforeStepCompletion(
        action: Action,
        continuation: Continuation<*>
    ) {
        if (
            continuation.validationBehavior == ValidationBehavior.AllowInvalid
            || (continuation.validationBehavior == ValidationBehavior.Default && continuation.statusBehavior != StatusBehavior.Complete)
        ) {
            return
        }

        runValidate(action, continuation.validationGroups)
    }

    fun validateBeforeAction(
        action: Action
    ) {
        if (
            action.definition.beforeExecutionValidation.let {
                it == ValidationBehavior.OnlyValid || (it == ValidationBehavior.Default && validateBeforeActions)
            }
        ) {
            runValidate(action, action.definition.validationGroups)
        }
    }

    private fun runValidate(
        action: Action,
        groups: Set<KClass<*>>
    ) {
        val step = action.step
        if (step is StatefulStep) {
            val allValidationIssues = step.data
                .mapNotNull { it.definition.validation?.create(it) }
                .flatMap { it.validate(groups) }

            if (allValidationIssues.isNotEmpty()) {
                throw DataValidationException(
                    "Validation failed for step \"${step.identifier}\" of workflow \"${step.workflow.id}\"",
                    action,
                    allValidationIssues
                )
            }
        }
    }
}