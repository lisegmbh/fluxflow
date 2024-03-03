package de.lise.fluxflow.api.ioc

import kotlin.reflect.KClass
import kotlin.reflect.KType

interface IocProvider {
    fun <TDependency: Any> provide(type: KClass<out TDependency>): TDependency?

    fun provide(type: KType): Any?
}