package de.lise.fluxflow.api.event

/**
 * Service to publish events.
 */
fun interface EventService {
    fun publish(event: FlowEvent)
}