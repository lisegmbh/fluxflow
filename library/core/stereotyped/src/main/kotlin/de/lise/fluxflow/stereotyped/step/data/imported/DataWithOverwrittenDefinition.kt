package de.lise.fluxflow.stereotyped.step.data.imported

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.Data

class DataWithOverwrittenDefinition<T>(
    override val definition: ImportedDataDefinition<T>,
    private val original: Data<T>,
) : Data<T> {
    override val step: Step
        get() = original.step

    override fun get(): T {
        return original.get()
    }
}