package de.lise.fluxflow.api.proxy.step.action

import de.lise.fluxflow.api.proxy.ProxyBuilder
import de.lise.fluxflow.api.step.stateful.StatefulStepDefinition
import kotlin.reflect.KClass

interface ActionProxyFactory{
    fun <T : Any> appendActionProxies(
        stepDefinition: StatefulStepDefinition,
        clazz: KClass<T>,
        parentBuilder: ProxyBuilder
    ): ProxyBuilder
}