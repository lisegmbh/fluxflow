package de.lise.fluxflow.engine.event.step

import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.workflow.Workflow

data class StepUpdatedEvent(val step: Step) : FlowEvent {
    override val workflow: Workflow<*>
        get() = step.workflow
    override val context: Any?
        get() = null
}