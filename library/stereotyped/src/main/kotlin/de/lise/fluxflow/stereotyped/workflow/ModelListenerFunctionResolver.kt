package de.lise.fluxflow.stereotyped.workflow

import de.lise.fluxflow.api.workflow.Workflow
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import de.lise.fluxflow.reflection.activation.function.FunctionResolver
import de.lise.fluxflow.reflection.activation.parameter.*
import de.lise.fluxflow.stereotyped.DefaultResolvers
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class ModelListenerFunctionResolver<TInstance: Any, TModel>(
    parameterResolver: ParameterResolver,
    private val function: KFunction<*>,
    modelType: KClass<TInstance>,
    workflowParameterProvider: ParameterProvider<Workflow<TModel>>,
    instanceParameterProvider: ParameterProvider<TInstance>,
    oldModelParameterProvider: ParameterProvider<Any?>,
    newModelParameterProvider: ParameterProvider<Any?>
) {
    private val isModelParameter = ParamMatcher.isAssignableFrom(modelType)

    private val isOldModelParameter = isModelParameter
        .and(
            ParamMatcher.hasNameContaining("old")
                .or(ParamMatcher.hasNameContaining("original"))
        )
    private val isFirstParameter = isModelParameter
        .and(ParamMatcher.isAtIndexAmongMatching(isModelParameter, 0))

    private val isNewModelParameter = isModelParameter
        .and(
            ParamMatcher.hasNameContaining("new")
                .or(ParamMatcher.hasNameContaining("updated"))
                .or(ParamMatcher.isAtIndexAmongMatching(isModelParameter, 1))
        )
    private val isSecondParameter = isModelParameter
        .and(ParamMatcher.isAtIndexAmongMatching(isModelParameter, 1))

    protected val functionResolver: FunctionResolver = BasicFunctionResolver(
        PriorityParameterResolver(
            CallbackParameterResolver(
                ParamMatcher.isFunctionInstance(),
                instanceParameterProvider
            ),
            CallbackParameterResolver(
                isOldModelParameter,
                oldModelParameterProvider
            ),
            CallbackParameterResolver(
                isNewModelParameter,
                newModelParameterProvider
            ),
            CallbackParameterResolver(
                isFirstParameter,
                oldModelParameterProvider
            ),
            CallbackParameterResolver(
                isSecondParameter,
                newModelParameterProvider
            ),
            *DefaultResolvers.forWorkflow(workflowParameterProvider),
            parameterResolver
        )
    )

    fun resolve(): FunctionResolution<*> {
        return functionResolver.resolve(function) ?: throw ModelListenerConfigurationException(
            "Could not resolve parameters for model listener function ${function.name}"
        )
    }
}