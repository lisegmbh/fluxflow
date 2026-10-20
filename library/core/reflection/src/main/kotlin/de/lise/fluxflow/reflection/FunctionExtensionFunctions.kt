package de.lise.fluxflow.reflection

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaConstructor

fun <T> KFunction<T>.isInvokableInstanceFunction(): Boolean {
    return this.visibility == KVisibility.PUBLIC && !this.isAbstract
}

fun <T> KFunction<T>.hasAdditionalParameters(): Boolean {
    return this.parameters.any{ it.kind != KParameter.Kind.INSTANCE }
}

val <T> KFunction<T>.declaringType: KType?
    get() = when {
        javaConstructor != null -> returnType
        instanceParameter != null -> instanceParameter?.type
        else -> null
    }