package de.lise.fluxflow.api.event

/**
 * A [FlowListener] can be used to react to certain workflow events.
 *
 * Because a listener is considered to execute workflow-external logic,
 * changes to a workflow must be performed using the regular FluxFlow APIs.
 *
 * **It is important** not to rely on changes being persisted implicitly.
 */
fun interface FlowListener {
    fun onFlowEvent(event: FlowEvent)
}