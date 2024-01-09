package de.lise.fluxflow.api.step.stateful

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.action.Action
import de.lise.fluxflow.api.step.stateful.data.Data

interface StatefulStep : Step {
    val data: List<Data<*>>
    val actions: List<Action>
}