package de.lise.fluxflow.api.workflow.action

import de.lise.fluxflow.api.step.stateful.action.ActionKind
import de.lise.fluxflow.api.workflow.Workflow

/**
 * Defines an action that can be executed in the context of an active workflow.
 */
interface WorkflowActionDefinition<TModel> {
    /**
     * The action kind that can be used to identify this action definition.
     */
    val kind: ActionKind

    /**
     * A simple key-value map providing additional metadata about this action definitions and actions executed based on it.
     *
     * This property is never `null` and does not contain `null` values.
     * The map will be empty if there is no metadata.
     */
    val metadata: Map<String, Any>

    /**
     * Creates a new executable action based on this definition.
     * @param workflow Specifies the workflow this action should be executed on.
     * @return An executable workflow action.
     */
    fun createAction(workflow: Workflow<TModel>): WorkflowAction<TModel>
}