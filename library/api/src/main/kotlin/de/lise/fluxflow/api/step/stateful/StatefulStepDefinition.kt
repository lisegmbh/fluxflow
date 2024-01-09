package de.lise.fluxflow.api.step.stateful

import de.lise.fluxflow.api.step.StepDefinition
import de.lise.fluxflow.api.step.stateful.action.ActionDefinition
import de.lise.fluxflow.api.step.stateful.data.DataDefinition

/**
 * A [StatefulStepDefinition] represents a kind of steps, that hold an internal state and are typically long-running.
 */
interface StatefulStepDefinition : StepDefinition {
    /**
     * Holds the definitions, that define the data that will be available during step execution.
     */
    val data: List<DataDefinition<*>>

    /**
     * Returns the definitions for actions that can be executed during step execution.
     */
    val actions: List<ActionDefinition>
}