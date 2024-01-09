package de.lise.fluxflow.stereotyped.step.action
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.api.step.Status

/**
 * Determines the [StatusBehavior] that should be set for continuations explicitly or implicitly
 * returned by action functions.
 *
 * In contrast to [StatusBehavior],
 * there is an additional [ImplicitStatusBehavior.Default]
 * that will keep the [StatusBehavior] that has been returned with the original continuation.
 */
enum class ImplicitStatusBehavior {
    /**
     * The step's status should always be set to [Status.Completed], regardless of the returned continuation.
     */
    Complete,
    /**
     * The step's status should always be set to [Status.Canceled], regardless of the returned continuation.
     */
    Cancel,
    /**
     * The step's status should always be preserved (not automatically updated),
     * regardless of the returned continuation.
     */
    Preserve,
    /**
     * The decision if the step's status should be preserved or automatically updated is decided by the returned continuation.
      */
    Default
}