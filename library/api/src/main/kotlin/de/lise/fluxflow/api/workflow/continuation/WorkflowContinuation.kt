package de.lise.fluxflow.api.workflow.continuation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.ContinuationType
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.api.validation.ValidationBehavior
import kotlin.reflect.KClass

data class WorkflowContinuation<TWorkflowModel, out TContinuation>(
    val initialWorkflowContinuation: Continuation<TContinuation>,
    override val model: TWorkflowModel,
    override val statusBehavior: StatusBehavior,
    override val validationBehavior: ValidationBehavior,
    val forkBehavior: ForkBehavior,
    override val validationGroups: Set<KClass<*>>
): Continuation<TWorkflowModel> {
    override val type: ContinuationType = ContinuationType.Workflow
    
    override fun withStatusBehavior(statusBehavior: StatusBehavior): Continuation<TWorkflowModel> {
        return copy(
            statusBehavior = statusBehavior
        )
    }

    override fun withValidationBehavior(validationBehavior: ValidationBehavior): Continuation<TWorkflowModel> {
        return copy(
            validationBehavior = validationBehavior
        )
    }
    
    fun withForkBehavior(forkBehavior: ForkBehavior): WorkflowContinuation<TWorkflowModel, TContinuation> {
        return this.copy(
            forkBehavior = forkBehavior
        )
    }

    override fun withValidationGroups(groups: Set<KClass<*>>): Continuation<TWorkflowModel> {
        return this.copy(
            validationGroups = groups
        )
    }
}