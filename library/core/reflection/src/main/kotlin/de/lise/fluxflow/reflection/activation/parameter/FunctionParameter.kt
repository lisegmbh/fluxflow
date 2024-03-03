package de.lise.fluxflow.reflection.activation.parameter

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

data class FunctionParameter<T>(
    val function: KFunction<T>,
    val param: KParameter
)