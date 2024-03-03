package de.lise.fluxflow.reflection.activation.function

import kotlin.reflect.KFunction

interface FunctionResolver {
    fun <TResult> resolve(function: KFunction<TResult>): FunctionResolution<TResult>?
}