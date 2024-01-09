package de.lise.fluxflow.engine.event.step.data

import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.workflow.Workflow

data class StepDataEvent(
    val stepData: Data<*>,
    val oldValue: Any?,
    val newValue: Any?,
) : FlowEvent {
    override val workflow: Workflow<*>
        get() = stepData.step.workflow
}
