package de.lise.fluxflow.stereotyped.step.automation

import de.lise.fluxflow.api.step.Status

/**
 * This enum represents the events within a step's lifecycle at wich automation can be run.
 */
enum class Trigger {
    /**
     * Specifies that the automation should run whenever the step is initially started.
     */
    OnCreated,

    /**
     * Specifies that the automation should run whenever the step transitions into the [Status.Completed] status.
     */
    OnCompleted
}