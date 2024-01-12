package de.lise.fluxflow.api.proxy.step.data

import de.lise.fluxflow.api.proxy.ProxyBuilder
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import kotlin.reflect.KClass

interface DataProxyFactory {
    fun <T> appendDataProxies(
        stepDefinition: StatefulStepDefinition,
        clazz: KClass<*>,
        builder: ProxyBuilder,
    ): ProxyBuilder
}