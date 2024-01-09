package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.step.Step

/**
 * A [DataListenerDefinition] defines a [DataListener] that can be used to listen for step data changes.
 * @param T The step data's model type.
 */
interface DataListenerDefinition<T> {
    /**
     * Creates a new [DataListener] based on this definition.
     * @param step The owning step.
     * @return A new [DataListener] instance.
     */
    fun create(step: Step): DataListener<T>
}