package de.lise.fluxflow.stereotyped.step.data.imported

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.step.stateful.data.ModifiableData

class DataWithDecoratedSetter<T>(
    private val readVariant: Data<T>,
    private val setter: (v: T) -> Unit,
) : ModifiableData<T> {
    override fun set(value: T) {
        setter(value)
    }

    override val step: Step
        get() = readVariant.step
    override val definition: DataDefinition<T>
        get() = readVariant.definition

    override fun get(): T {
        return readVariant.get()
    }
}