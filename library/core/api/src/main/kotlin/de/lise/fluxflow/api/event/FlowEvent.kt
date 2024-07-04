package de.lise.fluxflow.api.event

import de.lise.fluxflow.api.workflow.Workflow


/**
 * Marker interface for all events that can be published by the [EventService].
 */
interface FlowEvent {
    val workflow: Workflow<*>

    /**
     * Contains context information associated with the current execution.
     * May return `null`, if no context information has been provided.
     */
    val context: Any?
}