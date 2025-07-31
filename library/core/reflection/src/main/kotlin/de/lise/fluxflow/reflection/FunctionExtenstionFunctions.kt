package de.lise.fluxflow.reflection

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility

fun <T> KFunction<T>.isInvokableInstanceFunction(): Boolean {
    return this.visibility == KVisibility.PUBLIC && !this.isAbstract
}

fun <T> KFunction<T>.hasAdditionalParameters(): Boolean {
    return this.parameters.any{ it.kind != KParameter.Kind.INSTANCE }
}