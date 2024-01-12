package de.lise.fluxflow.api.proxy

import de.lise.fluxflow.api.proxy.step.StepProxyType
import de.lise.fluxflow.api.step.StepDefinition
import net.bytebuddy.dynamic.DynamicType
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

interface ProxyTypeFactory {
    fun <T : Any> createProxy(
        stepDefinition: StepDefinition,
        clazz: KClass<T>
    ): StepProxyType<T>
}

typealias ProxyBuilder = DynamicType.Builder<out Any>

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> ProxyTypeFactory.createProxy(
    stepDefinition: StepDefinition,
): StepProxyType<T> {
    val clazz = typeOf<T>().classifier as? KClass<T> ?: throw InvalidProxyInterfaceException(
        "The type ${typeOf<T>()} does not represent a class/interface."
    )

    return this.createProxy(
        stepDefinition,
        clazz
    )
}