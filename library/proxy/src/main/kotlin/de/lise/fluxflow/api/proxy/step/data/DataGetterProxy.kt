package de.lise.fluxflow.api.proxy.step.data

import de.lise.fluxflow.api.proxy.step.StepAccessor
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.StepDataService
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.This

class DataGetterProxy(
    private val stepDataService: StepDataService,
    private val dataKind: DataKind
) {
    @Suppress("unused") // Used by ByteBuddy
    @RuntimeType
    fun intercept(@This instance: StepAccessor): Any? {
        return stepDataService.getData<Any?>(
            instance._proxyStep,
            dataKind
        )!!.get()
    }
}