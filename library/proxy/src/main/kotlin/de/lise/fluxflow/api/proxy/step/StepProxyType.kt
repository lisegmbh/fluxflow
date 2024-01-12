package de.lise.fluxflow.api.proxy.step

import de.lise.fluxflow.api.step.Step

fun interface StepProxyType<T> {
    fun instantiateProxy(step: Step): T
}