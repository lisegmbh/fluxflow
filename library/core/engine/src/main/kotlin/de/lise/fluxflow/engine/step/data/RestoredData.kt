package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.DataDefinition

class RestoredData(
    override val step: Step,
    override val definition: DataDefinition<Any?>,
    private val value: Any?
) : Data<Any?> {
    override fun get(): Any? {
        return value  
    }
}