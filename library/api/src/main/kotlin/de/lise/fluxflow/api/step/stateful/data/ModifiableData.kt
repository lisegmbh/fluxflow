package de.lise.fluxflow.api.step.stateful.data

interface ModifiableData<T> : Data<T> {
    fun set(value: T)
}