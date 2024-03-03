package de.lise.fluxflow.stereotyped.step.action

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import de.lise.fluxflow.reflection.activation.parameter.CallbackParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.ParamMatcher
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.stereotyped.DefaultResolvers
import kotlin.reflect.KFunction

open class ActionFunctionResolverImpl(
    private val basicParameterResolver: ParameterResolver
) : ActionFunctionResolver {
    override fun <TFunctionOwner : Any> resolve(
        function: KFunction<*>,
        instanceProvider: () -> TFunctionOwner,
        stepProvider: () -> Step
    ): FunctionResolution<*> {
        val resolution = BasicFunctionResolver(
            PriorityParameterResolver(
                CallbackParameterResolver(
                    ParamMatcher.isFunctionInstance(),
                    instanceProvider
                ),
                *DefaultResolvers.forStep(stepProvider),
                basicParameterResolver,
            )
        ).resolve(function)

        return resolution ?: throw ActionConfigurationException(
            "Could not resolve parameters for action function ${function.name}"
        )
    }
}
