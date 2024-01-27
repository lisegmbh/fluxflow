package de.lise.fluxflow.api.step.stateful

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.data.Data

/**
 * A [StatefulStep] is a step with associated state information.
 * Such a step can define and hold data and provide executable actions.
 */
interface StatefulStep : Step {
    /**
     * The data that is stored for this step.
     */
    val data: List<Data<*>>

    /**
     * A list of actions that are executable for this step.
     */
    val actions: List<Action>
}