package de.lise.fluxflow.api.step.continuation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

/**
 * This [Continuation] indicates that the workflow should be continued with the step provided by [model].
 */
class StepContinuation<T>(
    override val model: T,
    override val statusBehavior: StatusBehavior = StatusBehavior.Complete,
    override val validationBehavior: ValidationBehavior = ValidationBehavior.Default,
    override val validationGroups: Set<KClass<*>>
): Continuation<T> {
    override val type: ContinuationType = ContinuationType.Step
    
    override fun withStatusBehavior(statusBehavior: StatusBehavior): Continuation<T> {
        return StepContinuation(
            model,
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<T> {
        return StepContinuation(
            model,
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationGroups(groups: Set<KClass<*>>): Continuation<T> {
        return StepContinuation(
            model,
            statusBehavior,
            validationBehavior,
            groups
        )
    }
}