package de.lise.fluxflow.api.event

fun interface FlowListener {
    fun onFlowEvent(event: FlowEvent)
}