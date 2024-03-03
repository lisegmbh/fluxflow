package de.lise.fluxflow.stereotyped.step.automation

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import de.lise.fluxflow.reflection.activation.function.FunctionResolver
import de.lise.fluxflow.reflection.activation.parameter.*
import de.lise.fluxflow.stereotyped.DefaultResolvers
import kotlin.reflect.KFunction

/**
 * The [AutomationFunctionResolver] can be
 * used to resolve the actual parameters that are required in order to invoke an automation function.
 * @param TInstance The type that defines the function that's parameters should be resolved.
 */
class AutomationFunctionResolver<TInstance : Any>(
    parameterResolver: ParameterResolver,
    private val function: KFunction<*>,
    instanceParameterProvider: ParameterProvider<TInstance>,
    stepParameterProvider: ParameterProvider<Step>
) {
    
    private val functionResolver: FunctionResolver = BasicFunctionResolver(
        PriorityParameterResolver(
            CallbackParameterResolver(
                ParamMatcher.isFunctionInstance(),
                instanceParameterProvider
            ),
            *DefaultResolvers.forStep(stepParameterProvider),
            parameterResolver
        )
    )
    
    fun resolve(): FunctionResolution<*> {
        return functionResolver.resolve(function) ?: throw AutomationConfigurationException(
            "Could not resolve parameters for automation function ${function.name}"
        )
    }
}