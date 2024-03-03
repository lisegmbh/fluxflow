package de.lise.fluxflow.api.step.stateful.action

import de.lise.fluxflow.api.step.Step

interface ActionService {
    fun getActions(step: Step): List<Action>
    fun getAction(step: Step, kind: ActionKind): Action?
    fun invokeAction(action: Action)
}