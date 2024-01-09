package de.lise.fluxflow.stereotyped.job

import de.lise.fluxflow.api.job.Job
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import de.lise.fluxflow.reflection.activation.function.FunctionResolver
import de.lise.fluxflow.reflection.activation.parameter.*
import de.lise.fluxflow.stereotyped.DefaultResolvers
import kotlin.reflect.KFunction

class PayloadFunctionResolver<TInstance : Any>(
    parameterResolver: ParameterResolver,
    private val function: KFunction<*>,
    instanceParameterProvider: ParameterProvider<TInstance>,
    jobParameterProvider: ParameterProvider<Job>
) {
    private val functionResolver: FunctionResolver = BasicFunctionResolver(
        PriorityParameterResolver(
            CallbackParameterResolver(
                ParamMatcher.isFunctionInstance(),
                instanceParameterProvider
            ),
            *DefaultResolvers.forJob(jobParameterProvider),
            parameterResolver
        )
    )

    fun resolve(): FunctionResolution<*> {
        return functionResolver.resolve(function)
            ?: throw JobConfigurationException(
                "Could not resolve parameters for payload function ${function.name}"
            )
    }
}