package de.lise.fluxflow.api.continuation

import de.lise.fluxflow.api.WorkflowObjectKind
import de.lise.fluxflow.api.continuation.reason.Reason
import de.lise.fluxflow.api.job.CancellationKey
import de.lise.fluxflow.api.job.continuation.JobCancellationContinuation
import de.lise.fluxflow.api.job.continuation.JobContinuation
import de.lise.fluxflow.api.step.continuation.StepContinuation
import de.lise.fluxflow.api.validation.ValidationBehavior
import de.lise.fluxflow.api.workflow.continuation.ForkBehavior
import de.lise.fluxflow.api.workflow.continuation.WorkflowContinuation
import java.time.Instant
import kotlin.reflect.KClass

/**
 * A [Continuation] can be returned by a workflow step action to indicate/control how the workflow should be continued.
 *
 * @param T The type of data which will be passed into the further workflow execution.
 */
interface Continuation<out T> {
    /**
     * The data and/or next workflow action.
     */
    val model: T

    /**
     * Determines what should happen with the originating step's status.
     */
    val statusBehavior: StatusBehavior

    /**
     * The type of this continuation.
     */
    val type: ContinuationType

    /**
     * Specifies the validation behavior that will be applied when this continuation is about to be executed.
     * If [ValidationBehavior.Default] this is set to [ValidationBehavior.Default],
     * validation will only be required to succeed if [Continuation.statusBehavior] is set to [StatusBehavior.Complete].
     */
    val validationBehavior: ValidationBehavior

    /**
     * The validation groups to be evaluated upon executing this continuation. 
     */
    val validationGroups: Set<KClass<*>>

    /**
     * The reason why this continuation was chosen, providing context for debugging and auditing.
     * Defaults to null for backward compatibility.
     */
    val reason: Reason? get() = null

    /**
     * Returns a new continuation with [Continuation.statusBehavior] set to [statusBehavior].
     * @param statusBehavior The behavior that should be applied to the originating step's behavior.
     * @return A new instance of this continuation.
     */
    fun withStatusBehavior(statusBehavior: StatusBehavior): Continuation<T>

    /**
     * Returns a new continuation with [Continuation.validationBehavior] set to [validationBehavior].
     * @param validationBehavior The validation behavior to be applied.
     * @return A new instance of this continuation.
     */
    fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<T>

    /***
     * Returns a new continuation with [Continuation.validationGroups] set to [validationGroups].
     * @param groups The validation groups to be applied when executing this continuation.
     * @return A new instance of this continuation.
     */
    fun withValidationGroups(groups: Set<KClass<*>>): Continuation<T>
    
    /***
     * Returns a new continuation with [Continuation.validationGroups] set to [validationGroups].
     * @param groups The validation groups to be applied when executing this continuation.
     * @return A new instance of this continuation.
     */
    fun withValidationGroups(vararg groups: KClass<*>): Continuation<T> {
        return withValidationGroups(setOf(*groups))
    }

    /**
     * Returns a new continuation with the specified reason.
     * @param reason The reason why this continuation was chosen.
     * @return A new instance of this continuation with the reason attached.
     */
    fun withReason(reason: Reason): Continuation<T> = ReasonedContinuation(this, reason)

    /**
     * Returns a new continuation with the specified reason message and optional context.
     * @param message A human-readable explanation of why this continuation was chosen.
     * @param context Additional structured data providing context for the reason.
     * @return A new instance of this continuation with the reason attached.
     */
    fun withReason(message: String, context: Map<String, Any?> = emptyMap()): Continuation<T> =
        withReason(Reason(message, context))

    companion object {
        /**
         * Returns a new step continuation, indicating that the workflow should continue with the step provided with [model].
         * @param model The step that should be executed next.
         * @param reason The reason why this continuation was chosen (optional).
         */
        fun <T> step(model: T, reason: Reason? = null): Continuation<T> {
            val base = StepContinuation(
                model,
                validationGroups = emptySet()
            )
            return reason?.let { ReasonedContinuation(base, it) } ?: base
        }

        /**
         * Returns a new job continuation, indicating that the job provided by the given [model] should be scheduled for
         * [scheduledTime].
         * @param scheduledTime The time at which the job should run.
         * @param model The job that should be executed.
         * @param cancellationKey The cancellation key that can be used to cancel this job.
         * If the job should not be cancelable, `null` might be used (default).
         * Note that all previously scheduled jobs having the same key are going to be canceled if such is specified.
         * @param reason The reason why this continuation was chosen (optional).
         */
        fun <T> job(
            scheduledTime: Instant,
            model: T,
            cancellationKey: CancellationKey? = null,
            reason: Reason? = null
        ): Continuation<T> {
            val base = JobContinuation(
                scheduledTime,
                cancellationKey,
                model,
                validationGroups = emptySet()
            )
            return reason?.let { ReasonedContinuation(base, it) } ?: base
        }

        /**
         * Returns a new continuation,
         * indicating that all scheduled jobs with the provided [cancellationKey] should be canceled.
         * @param cancellationKey The cancellation key that should be used to search for jobs to be canceled.
         * @param reason The reason why this continuation was chosen (optional).
         */
        fun cancelJobs(
            cancellationKey: CancellationKey,
            reason: Reason? = null
        ): Continuation<Unit> {
            val base = JobCancellationContinuation(
                cancellationKey,
                validationGroups = emptySet()
            )
            return reason?.let { ReasonedContinuation(base, it) } ?: base
        }

        /**
         * Returns a continuation indicating that there is no more work to be done.
         * @param reason The reason why this continuation was chosen (optional).
         */
        fun none(reason: Reason? = null): Continuation<*> {
            val base = NoContinuation(
                validationGroups = emptySet()
            )
            return reason?.let { ReasonedContinuation(base, it) } ?: base
        }

        /**
         * Returns a continuation that wraps all given [continuations]
         * and can be used if the workflow should be continued with multiple operations.
         * @param reason The reason why this continuation was chosen (optional).
         */
        fun multiple(vararg continuations: Continuation<*>, reason: Reason? = null): Continuation<Unit> {
            val base = MultipleContinuation(
                continuations.toSet(),
                validationGroups = emptySet(),
            )
            return reason?.let { ReasonedContinuation(base, it) } ?: base
        }

        /**
         * Returns a continuation that wraps all given [continuations]
         * and can be used if the workflow should be continued with multiple operations.
         * @param reason The reason why this continuation was chosen (optional).
         */
        fun multiple(continuations: Collection<Continuation<*>>, reason: Reason? = null): Continuation<Unit> {
            val base = MultipleContinuation(
                continuations.toSet(),
                validationGroups = emptySet()
            )
            return reason?.let { ReasonedContinuation(base, it) } ?: base
        }

        /**
         * Returns a continuation that reactivates the previous step.
         * By default, the current step is going to be canceled.
         * @param TPreviousStepModel Serves no purpose, except from documenting the previous step type.
         * Might be [Unit].
         * @param reason The reason why this continuation was chosen (optional).
         * @return a [RollbackContinuation].
         */
        fun <TPreviousStepModel> rollback(reason: Reason? = null): Continuation<Unit> {
            val base = RollbackContinuation(validationGroups = emptySet())
            return reason?.let { ReasonedContinuation(base, it) } ?: base
        }

        /**
         * Returns a continuation
         * that will start a new workflow having the specified [workflowModel]
         * and begins executing the [initialWorkflowContinuation].
         * @param TWorkflowModel The type of new workflow's model.
         * @param TContinuation The initial continuations model.
         * @param workflowModel The initial value for the new workflow's model.
         * @param initialWorkflowContinuation The initial continuation that should be executed when starting the new workflow.
         * @param forkBehavior The fork behavior controlling the controls what is happening to the old/original workflow.
         * @param reason The reason why this continuation was chosen (optional).
         */
        fun <TWorkflowModel, TContinuation> workflow(
            workflowModel: TWorkflowModel,
            initialWorkflowContinuation: Continuation<TContinuation>,
            forkBehavior: ForkBehavior = ForkBehavior.Fork,
            replacementScope: Set<WorkflowObjectKind> = emptySet(),
            reason: Reason? = null
        ): Continuation<TWorkflowModel> {
            val base = WorkflowContinuation(
                initialWorkflowContinuation,
                workflowModel,
                StatusBehavior.Complete,
                ValidationBehavior.Default,
                forkBehavior,
                validationGroups = emptySet(),
                replacementScope
            )
            return reason?.let { ReasonedContinuation(base, it) } ?: base
        }
    }
}