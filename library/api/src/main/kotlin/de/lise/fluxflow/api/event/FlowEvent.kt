package de.lise.fluxflow.api.event

import de.lise.fluxflow.api.workflow.Workflow


/**
 * Marker interface for all events that can be published by the [EventService].
 */
interface FlowEvent {
    val workflow: Workflow<*>
}