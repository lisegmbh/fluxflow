package de.lise.fluxflow.api.step.stateful.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step

/**
 * Service interface for retrieving and invoking workflow actions associated with a step.
 *
 * An *action* represents a unit of executable behavior tied to a step in a workflow.
 * Actions may originate from various sources — such as annotated Kotlin functions, external configuration
 * files (e.g., XML), or other mechanisms — depending on the workflow engine’s implementation.
 *
 * This interface abstracts away the underlying definition mechanism and provides a unified API for working
 * with actions at runtime.
 */
interface ActionService {
    /**
     * Returns all executable actions associated with the given [step].
     *
     * The origin and discovery mechanism of the actions (e.g., annotations, configuration, metadata)
     * is not specified by this API and may vary depending on the implementation.
     *
     * @param step The step whose actions should be retrieved.
     * @return A list of [Action] objects representing executable behavior.
     */
    fun getActions(step: Step): List<Action>

    /**
     * Retrieves the action of the given [kind] associated with the provided [step].
     *
     * Throws an exception if no such action exists.
     *
     * @param step The step containing the action.
     * @param kind The kind identifier of the action.
     * @return The matching [Action].
     * @throws ActionNotFoundException if there is no action for the given [step] and [kind].
     */
    fun getAction(step: Step, kind: ActionKind): Action

    /**
     * Retrieves the action of the given [kind] associated with the provided [step], or returns `null` if not found.
     *
     * @param step The step containing the action.
     * @param kind The kind identifier of the action.
     * @return The matching [Action], or `null` if not available.
     */
    fun getActionOrNull(step: Step, kind: ActionKind): Action?

    /**
     * Invokes the given [action] and returns the resulting [Continuation].
     *
     * **Note:** The continuation has already been processed and applied by the engine.
     * You may use the returned value to inspect the outcome of the action.
     *
     * @param action The action to execute.
     * @return The [Continuation] that was produced and already executed.
     */
    fun invokeAction(action: Action): Continuation<*>

    /**
     * Resolves and invokes the action of the given [kind] on the specified [step].
     *
     * Internally uses [getAction] followed by [invokeAction].
     *
     * **Note:** The returned [Continuation] has already been executed by the engine.
     *
     * @param step The step that contains the action.
     * @param kind The identifier of the action to invoke.
     * @return The [Continuation] that was produced and already executed.
     * @throws ActionNotFoundException if the action cannot be found.
     */
    fun invokeAction(step: Step, kind: ActionKind): Continuation<*> {
        return invokeAction(
            getAction(
                step,
                kind
            )
        )
    }
}