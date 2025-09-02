package de.lise.fluxflow.engine.event.action

import de.lise.fluxflow.api.event.FlowEvent
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.workflow.Workflow

data class ActionEvent(val action: Action) : FlowEvent {
    override val workflow: Workflow<*>
        get() = action.step.workflow
    override val context: Any?
        get() = null
}