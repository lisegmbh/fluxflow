package de.lise.fluxflow.api.job.continuation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

class JobCancellationContinuation(
    val cancellationKey: CancellationKey,
    override val statusBehavior: StatusBehavior = StatusBehavior.Complete,
    override val validationBehavior: ValidationBehavior = ValidationBehavior.Default,
    override val validationGroups: Set<KClass<*>>
) : Continuation<Unit> {
    override val model: Unit
        get() = Unit
    override val type: ContinuationType = ContinuationType.JobCancellation

    override fun withStatusBehavior(statusBehavior: StatusBehavior): Continuation<Unit> {
        return JobCancellationContinuation(
            cancellationKey,
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<Unit> {
        return JobCancellationContinuation(
            cancellationKey,
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationGroups(groups: Set<KClass<*>>): Continuation<Unit> {
        return JobCancellationContinuation(
            cancellationKey,
            statusBehavior,
            validationBehavior,
            groups
        )
    }
}