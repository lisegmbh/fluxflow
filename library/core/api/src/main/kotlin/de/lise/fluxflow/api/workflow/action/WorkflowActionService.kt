package de.lise.fluxflow.api.workflow.action

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
     * @return The [WorkflowAction], or `null` if no action with the given [kind] is available.
     * @throws WorkflowActionNotFoundException if there is no action of the specified [kind].
     */
    fun <TModel : Any> getAction(workflow: Workflow<TModel>, kind: ActionKind): WorkflowAction<TModel>

    /**
     * Executes the given [action] while handling all required side effects.
     *
     * @param action The action to be executed.
     */
    fun <TModel : Any> invokeAction(action: WorkflowAction<TModel>)

    /**
     * Execute a workflow action based on its [kind].
     *
     * **Note** that this is a convenience overload.
     * One could also fetch the action first (using [getAction])
     * and then execute it using the other [invokeAction] overload.
     *
     * @param TModel The workflow model's type.
     * @param workflow The workflow from which the action should be obtained.
     * @param kind The kind of the action to be executed.
     */
    fun <TModel : Any> invokeAction(workflow: Workflow<TModel>, kind: ActionKind) {
        invokeAction(
            getAction(
                workflow,
                kind
            )
        )
    }
}