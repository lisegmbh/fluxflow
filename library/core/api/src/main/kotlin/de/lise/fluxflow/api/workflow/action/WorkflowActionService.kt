package de.lise.fluxflow.api.workflow.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.workflow.Workflow

/**
 * Service for obtaining and executing workflow actions.
 */
interface WorkflowActionService {
    /**
     * Returns the list of [WorkflowAction]s available for the given [workflow].
     *
     * @param TModel The workflow model's type.
     * @param workflow The workflow for which the available actions should be returned.
     * @return A list of available workflow actions. Might be empty but never `null`.
     */
    fun <TModel : Any> getActions(workflow: Workflow<TModel>): List<WorkflowAction<TModel>>

    /**
     * Returns a workflow action of the given [kind].
     *
     * @param TModel The workflow model's type.
     * @param workflow The workflow from which the action should be obtained.
     * @param kind The expected [de.lise.fluxflow.api.step.stateful.action.ActionKind].
     * @return The [WorkflowAction] with the given [kind].
     * @throws WorkflowActionNotFoundException if there is no action of the specified [kind].
     */
    fun <TModel : Any> getAction(workflow: Workflow<TModel>, kind: ActionKind): WorkflowAction<TModel>

    /**
     * Returns a workflow action of the given [kind] or `null`, if no such action exists.
     *
     * @param TModel The workflow model's type.
     * @param workflow The workflow from which the action should be obtained.
     * @param kind The expected [de.lise.fluxflow.api.step.stateful.action.ActionKind].
     * @return The [WorkflowAction], or `null` if no action with the given [kind] is available.
     */
    fun <TModel : Any> getActionOrNull(workflow: Workflow<TModel>, kind: ActionKind): WorkflowAction<TModel>?

    /**
     * Invokes the given workflow [action] and returns the resulting [Continuation].
     *
     * **Note:** The continuation has already been fully executed by the workflow engine.
     * It is returned only for inspection or post-processing purposes.
     * Consumers must not use it to manipulate the workflow state manually.
     *
     * @param action The workflow action to invoke.
     * @return The [Continuation] that was produced and already executed.
     */
    fun <TModel : Any> invokeAction(action: WorkflowAction<TModel>): Continuation<*>

    /**
     * Resolves and invokes the workflow action of the given [kind] on the specified [workflow].
     *
     * Internally delegates to [getAction] and [invokeAction].
     *
     * **Note:** The returned [Continuation] has already been executed.
     *
     * @param TModel The workflow model's type.
     * @param workflow The workflow on which the action should be executed.
     * @param kind The kind identifier of the action to invoke.
     * @return The [Continuation] that was produced and already executed.
     */
    fun <TModel : Any> invokeAction(workflow: Workflow<TModel>, kind: ActionKind): Continuation<*> {
        return invokeAction(
            getAction(
                workflow,
                kind
            )
        )
    }
}