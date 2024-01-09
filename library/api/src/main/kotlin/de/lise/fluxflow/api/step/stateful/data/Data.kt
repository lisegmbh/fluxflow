package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.step.Step

interface Data<T> {
    val step: Step
    val definition: DataDefinition<T>
    fun get(): T
}