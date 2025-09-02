package de.lise.fluxflow.engine.event.step

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.workflow.Workflow

class StepUpdatedEvent(step: Step) : StepEvent(step) {
    override val workflow: Workflow<*>
        get() = step.workflow
    override val context: Any?
        get() = null
}