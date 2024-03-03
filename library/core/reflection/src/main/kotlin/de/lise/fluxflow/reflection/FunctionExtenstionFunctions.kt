package de.lise.fluxflow.reflection

import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility

fun <T> KFunction<T>.isInvokableInstanceFunction(): Boolean {
    return this.visibility == KVisibility.PUBLIC && !this.isAbstract
}