package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.step.Step

interface StepDataService {
    fun getData(step: Step): List<Data<*>>
    fun <T> getData(step: Step, kind: DataKind): Data<T>?
    fun <T> setValue(data: Data<T>, value: T)

    /**
     * Assigns the given [value] to the [data] while also providing extra [context] information.
     *
     * @param context Arbitrary information to be passed along.
     * @param value The value to be assigned.
     * @param data The data to be updated.
     */
    fun <T> setValue(context: Any, data: Data<T>, value: T)
}