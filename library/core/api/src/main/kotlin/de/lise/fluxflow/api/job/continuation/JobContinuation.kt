package de.lise.fluxflow.api.job.continuation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.validation.ValidationBehavior
import java.time.Instant
import kotlin.reflect.KClass

/**
 * This [Continuation] indicates that a job defined by [model] should be scheduled for this workflow.
 *
 * @param scheduledTime The time the job should be executed.
 * @param cancellationKey A cancellation key (if the job is supposed to be cancellable) or `null`.
 * @param model The job definition model.
 */
class JobContinuation<TJobModel>(
    val scheduledTime: Instant,
    val cancellationKey: CancellationKey?,
    override val model: TJobModel,
    override val statusBehavior: StatusBehavior = StatusBehavior.Complete,
    override val validationBehavior: ValidationBehavior = ValidationBehavior.Default,
    override val validationGroups: Set<KClass<*>>
) : Continuation<TJobModel> {
    override val type: ContinuationType = ContinuationType.Job
    
    fun withCancellationKey(key: CancellationKey?): JobContinuation<TJobModel> {
        return JobContinuation(
            scheduledTime,
            key,
            model,
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withStatusBehavior(statusBehavior: StatusBehavior): JobContinuation<TJobModel> {
        return JobContinuation(
            scheduledTime,
            cancellationKey,
            model,
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }


    override fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<TJobModel> {
        return JobContinuation(
            scheduledTime,
            cancellationKey,
            model,
            statusBehavior,
            validationBehavior,
            validationGroups
        )
    }

    override fun withValidationGroups(groups: Set<KClass<*>>): Continuation<TJobModel> {
        return JobContinuation(
            scheduledTime,
            cancellationKey,
            model,
            statusBehavior,
            validationBehavior,
            groups
        )
    }
}