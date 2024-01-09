package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.reflection.activation.function.BasicFunctionResolver
import de.lise.fluxflow.reflection.activation.function.FunctionResolution
import de.lise.fluxflow.reflection.activation.function.FunctionResolver
import de.lise.fluxflow.reflection.activation.parameter.*
import de.lise.fluxflow.stereotyped.DefaultResolvers
import java.lang.reflect.Type
import kotlin.reflect.KFunction

class DataListenerFunctionResolver<TInstance : Any>(
    parameterResolver: ParameterResolver,
    private val function: KFunction<*>,
    dataKind: DataKind,
    dataType: Type,
    stepParameterProvider: ParameterProvider<Step>,
    instanceParameterProvider: ParameterProvider<TInstance>,
    oldValueParameterProvider: ParameterProvider<Any?>,
    newValueParameterProvider: ParameterProvider<Any?>
) {

    private val isDataParameter = ParamMatcher.isAssignableFrom(dataType)

    private val isOldValueParameter = isDataParameter
        .and(
            ParamMatcher.hasNameContaining("old")
                .or(ParamMatcher.hasNameContaining("original"))
        )
    private val isFirstParameter = isDataParameter
        .and(ParamMatcher.isAtIndexAmongMatching(isDataParameter, 0))


    private val isNewValueParameter = isDataParameter
        .and(
            ParamMatcher.hasNameContaining("new")
                .or(ParamMatcher.hasNameContaining("updated"))
                .or(ParamMatcher.isAtIndexAmongMatching(isDataParameter, 1))
        )
    private val isSecondParameter = isDataParameter
        .and(ParamMatcher.isAtIndexAmongMatching(isDataParameter, 1))

    private val functionResolver: FunctionResolver = BasicFunctionResolver(
        PriorityParameterResolver(
            CallbackParameterResolver(
                ParamMatcher.isFunctionInstance(),
                instanceParameterProvider
            ),
            CallbackParameterResolver(
                isOldValueParameter,
                oldValueParameterProvider,
            ),
            CallbackParameterResolver(
                isNewValueParameter,
                newValueParameterProvider,
            ),
            CallbackParameterResolver(
                isFirstParameter,
                oldValueParameterProvider,
            ),
            CallbackParameterResolver(
                isSecondParameter,
                newValueParameterProvider
            ),
            CallbackParameterResolver(
                ParamMatcher.isAssignableFrom(DataKind::class)
            ) { dataKind },
            *DefaultResolvers.forStep(stepParameterProvider),
            parameterResolver
        )
    )

    fun resolve(): FunctionResolution<*> {
        return functionResolver.resolve(function)
            ?: throw DataListenerConfigurationException(
                "Could not resolve parameters for data listener function ${function.name}"
            )
    }
}