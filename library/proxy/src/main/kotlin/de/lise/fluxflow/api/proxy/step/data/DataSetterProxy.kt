package de.lise.fluxflow.api.proxy.step.data

import de.lise.fluxflow.api.proxy.step.StepAccessor
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.StepDataService
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.This

class DataSetterProxy(
    private val stepDataService: StepDataService,
    private val dataKind: DataKind
) {
    @Suppress("unused") // Used by ByteBuddy
    fun intercept(@This instance: StepAccessor, @Argument(0) value: Any?) {
        val data = stepDataService.getData<Any?>(
            instance._proxyStep,
            dataKind
        )!!
        stepDataService.setValue(data, value)
    }
}