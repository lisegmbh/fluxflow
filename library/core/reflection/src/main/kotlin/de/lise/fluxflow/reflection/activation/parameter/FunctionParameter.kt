package de.lise.fluxflow.reflection.activation.parameter

import de.lise.fluxflow.reflection.declaringType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

data class FunctionParameter<T>(
    val function: KFunction<T>,
    val param: KParameter
) {
    val matchingProperty: KProperty1<*,*>? = when (param.name) {
        null -> null
        else -> (function.declaringType?.classifier as? KClass<*>)
            ?.memberProperties
            ?.firstOrNull { property ->
                property.name == param.name
            }
    }
}