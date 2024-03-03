package de.lise.fluxflow.engine.event

import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.event.FlowListener

class EventServiceImpl(
    private val listeners: List<FlowListener>
) : EventService {
    override fun publish(event: FlowEvent) {
        listeners.forEach {
            it.onFlowEvent(event)
        }
    }
}