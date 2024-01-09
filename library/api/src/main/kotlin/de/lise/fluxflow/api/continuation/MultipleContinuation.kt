package de.lise.fluxflow.api.continuation

import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

class MultipleContinuation(
    val continuations: Set<Continuation<*>>,
    // Should always be set to StatusBehavior.Preserve,
    // because the actual continuation is decided by the wrapped continuations
    override val statusBehavior: StatusBehavior = StatusBehavior.Preserve,
    override val validationBehavior: ValidationBehavior = ValidationBehavior.Default,
    override val validationGroups: Set<KClass<*>>
) : Continuation<Unit> {
    override val model: Unit
        get() = Unit

    override val type: ContinuationType = ContinuationType.Multiple

    override fun withStatusBehavior(statusBehavior: StatusBehavior): Continuation<Unit> {
        return MultipleContinuation(
            continuations.map { it.withStatusBehavior(statusBehavior) }.toSet(),
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<Unit> {
        return MultipleContinuation(
            continuations.map { it.withValidationBehavior(validationBehavior) }.toSet(),
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationGroups(groups: Set<KClass<*>>): Continuation<Unit> {
        return MultipleContinuation(
            continuations,
            statusBehavior,
            validationBehavior,
            groups
        )
    }
}