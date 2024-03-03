package de.lise.fluxflow.reflection

import java.lang.reflect.Type
import kotlin.reflect.KClass

fun Type.toKClass(): KClass<*>? {
    return (this as? Class<*>)?.kotlin
}