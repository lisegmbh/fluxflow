package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.continuation.Continuation

/**
 * A [DataListener] is a listener that is invoked whenever certain step data is updated.
 */
interface DataListener<T> {
    /**
     * The definition of this listener.
     */
    val definition: DataListenerDefinition<T>

    /**
     * This method to be invoked when the subscribed step data changed.
     * @param oldValue The step data's original value (before the update occurred).
     * @param newValue The step data's new value (after the update occurred).
     * @return A continuation that determines how the workflow should be continued.
     */
    fun onChange(oldValue: T, newValue: T): Continuation<*>
}