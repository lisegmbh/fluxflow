package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import kotlin.reflect.KFunction

interface ActionFunctionResolver {
    fun <TFunctionOwner : Any> resolve(
        function: KFunction<*>,
        instanceProvider: () -> TFunctionOwner,
        stepProvider: () -> Step
    ): FunctionResolution<*>
}