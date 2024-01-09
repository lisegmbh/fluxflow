package de.lise.fluxflow.api.step.automation

import de.lise.fluxflow.api.step.Step

/**
 * An automation definition is a template for a specific [Automation].
 * It can be used to create an instance of [Automation] that will be associated to a step.
 */
interface AutomationDefinition {
    /**
     * Creates a new automation based on this definition.
     * @param step The step this automation will be associated with.
     * @return An [Automation] that is based on this definition and associated with the given step.
     */
    fun createAutomation(step: Step): Automation
}