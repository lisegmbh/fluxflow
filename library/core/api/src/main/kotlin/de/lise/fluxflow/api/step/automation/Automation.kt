package de.lise.fluxflow.api.step.automation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step

/**
 * An automation is a piece of logic that is run automatically during a step's lifecycle.
 */
interface Automation {
    /**
     * The step this automation belongs to.
     */
    val step: Step
    /**
     * The definition of this automation.
     */
    val definition: AutomationDefinition
    /**
     * Executes the logic represented by this automation.
     * @return The continuation that is returned by the automation.
     */
    fun execute(): Continuation<*>
}

