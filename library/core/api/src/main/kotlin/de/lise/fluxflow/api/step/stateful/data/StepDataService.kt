package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.step.Step

interface StepDataService {
    fun getData(step: Step): List<Data<*>>
    fun <T> getData(step: Step, kind: DataKind): Data<T>?
    fun <T> setValue(data: Data<T>, value: T)
}