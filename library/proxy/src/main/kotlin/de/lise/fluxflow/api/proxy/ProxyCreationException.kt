package de.lise.fluxflow.api.proxy

import kotlin.reflect.KClass

open class ProxyCreationException(message: String) : Exception(message) {
    internal companion object {
        fun finalClass(type: KClass<*>): ProxyCreationException {
            return InvalidProxyInterfaceException(
                "Can not create proxy for final class ${type.qualifiedName}, as it is final. " +
                        "Consider removing the \"final\" modifier (Java) or the adding the \"open\" modifier (Kotlin)."
            )
        }
    }
}