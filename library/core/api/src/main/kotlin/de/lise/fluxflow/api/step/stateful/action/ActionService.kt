package de.lise.fluxflow.api.step.stateful.action

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
     * Invokes the given [action].
     *
     * The implementation is responsible for providing any necessary parameter injection,
     * continuation handling, and lifecycle management.
     *
     * @param action The action to invoke.
     */
    fun invokeAction(action: Action)

    /**
     * Convenience method to invoke the action of the specified [kind] on the given [step].
     *
     * Internally retrieves the corresponding [Action] and then invokes it.
     *
     * @param step The step containing the action.
     * @param kind The kind identifier of the action.
     * @throws ActionNotFoundException if there is no action for the given [step] and [kind].
     */
    fun invokeAction(step: Step, kind: ActionKind) {
        invokeAction(
            getAction(
                step,
                kind
            )
        )
    }
}