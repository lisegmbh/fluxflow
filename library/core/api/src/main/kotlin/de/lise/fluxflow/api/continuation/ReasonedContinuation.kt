package de.lise.fluxflow.api.continuation

import de.lise.fluxflow.api.continuation.reason.Reason
import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

/**
 * A [ReasonedContinuation] wraps another [Continuation] and adds a reason explaining why this continuation was chosen.
 * This decorator preserves all the original continuation's behavior while providing additional context.
 *
 * @param delegate The original continuation to wrap.
 * @param reason The reason why this continuation was chosen.
 */
class ReasonedContinuation<T>(
    private val delegate: Continuation<T>,
    override val reason: Reason
) : Continuation<T> {
    override val model: T get() = delegate.model

    override val statusBehavior: StatusBehavior get() = delegate.statusBehavior

    override val type: ContinuationType get() = delegate.type

    override val validationBehavior: ValidationBehavior get() = delegate.validationBehavior

    override val validationGroups: Set<KClass<*>> get() = delegate.validationGroups

    override fun withStatusBehavior(statusBehavior: StatusBehavior): Continuation<T> {
        return ReasonedContinuation(
            delegate.withStatusBehavior(statusBehavior),
            reason
        )
    }

    override fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<T> {
        return ReasonedContinuation(
            delegate.withValidationBehavior(validationBehavior),
            reason
        )
    }

    override fun withValidationGroups(groups: Set<KClass<*>>): Continuation<T> {
        return ReasonedContinuation(
            delegate.withValidationGroups(groups),
            reason
        )
    }
}
