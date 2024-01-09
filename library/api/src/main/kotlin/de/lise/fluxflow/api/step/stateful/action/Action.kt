package de.lise.fluxflow.api.step.stateful.action

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.step.Step

interface Action {
    val step: Step
    val definition: ActionDefinition
    fun execute(): Continuation<*>
}