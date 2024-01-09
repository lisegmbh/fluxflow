package de.lise.fluxflow.api.continuation

import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

/**
 * A [RollbackContinuation] is used to indicate that the previous step should be canceled.
 * This is a common way to implement a "go back" workflow transition.
 * As such, the default status behavior is [StatusBehavior.Cancel].
 */
class RollbackContinuation(
    override val statusBehavior: StatusBehavior = StatusBehavior.Cancel,
    override val validationBehavior: ValidationBehavior = ValidationBehavior.Default,
    override val validationGroups: Set<KClass<*>>
) : Continuation<Unit> {
    override val model: Unit
        get() = Unit

    override val type: ContinuationType = ContinuationType.Rollback

    override fun withStatusBehavior(statusBehavior: StatusBehavior): Continuation<Unit> {
        return RollbackContinuation(
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<Unit> {
        return RollbackContinuation(
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationGroups(groups: Set<KClass<*>>): Continuation<Unit> {
        return RollbackContinuation(
            statusBehavior,
            validationBehavior,
            groups
        )
    }
}