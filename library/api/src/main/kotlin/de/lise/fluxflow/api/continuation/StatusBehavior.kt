package de.lise.fluxflow.api.continuation

import de.lise.fluxflow.api.step.Status

/**
 * The status behavior defines how a step's status should change after a continuation ran.
 */
enum class StatusBehavior {
    /**
     * Indicates that the step's status should be set to [Status.Completed].
     */
    Complete,

    /**
     * Indicates that the step's status should be set to [Status.Canceled].
     */
    Cancel,

    /**
     * Indicates that the step's status should not be changed automatically.
     * Effectively preserving the step's original status.
     */
    Preserve,
}