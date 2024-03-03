package de.lise.fluxflow.api.continuation

import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

/**
 * A [NoContinuation] can be used to indicate, that there is no more work to be done.
 * This effectively terminates the current execution branch.
 */
class NoContinuation(
    override val statusBehavior: StatusBehavior = StatusBehavior.Complete,
    override val validationBehavior: ValidationBehavior = ValidationBehavior.Default,
    override val validationGroups: Set<KClass<*>>
) : Continuation<Unit> {
    override val model: Unit
        get() = Unit

    override val type: ContinuationType = ContinuationType.Nothing

    override fun withStatusBehavior(statusBehavior: StatusBehavior): Continuation<Unit> {
        return NoContinuation(
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<Unit> {
        return NoContinuation(
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationGroups(groups: Set<KClass<*>>): Continuation<Unit> {
        return NoContinuation(
            statusBehavior,
            validationBehavior,
            groups
        )
    }
}