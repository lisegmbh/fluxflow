package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.DataListenerDefinition
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.step.action.ImplicitStatusBehavior
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.functions

class DataListenerDefinitionBuilder(
    private val continuationBuilder: ContinuationBuilder,
    private val parameterResolver: ParameterResolver,
) {
    fun <TFunctionOwner: Any, TProp> build(
        dataKind: DataKind,
        dataType: Type,
        instanceType: KClass<*>
    ): Set<(TFunctionOwner) -> DataListenerDefinition<TProp>> {
        return instanceType.functions
            .mapNotNull {
                build<TFunctionOwner, TProp>(
                    dataKind,
                    dataType,
                    it
                )
            }
            .toSet()
    }

    private fun <TFunctionOwner: Any, TProp> build(
        dataKind: DataKind,
        dataType: Type,
        function: KFunction<*>
    ): ((TFunctionOwner) -> DataListenerDefinition<TProp>)? {
        if(
            !canBeInvoked(function)
        ) {
            return null
        }

        val listenerAnnotations = function.findAnnotations<DataListener>()
            .filter {
                DataKind(it.dataKind) == dataKind
            }

        if(
            listenerAnnotations.isEmpty()
        ) {
            return null
        }

        val continuationConverter = continuationBuilder.createResultConverter(
            ImplicitStatusBehavior.Preserve,
            false,
            function
        )

        return {obj ->
            ReflectedDataListenerDefinition(
                obj
            ) { step, instance, old, new ->
                val callable = DataListenerFunctionResolver(
                    parameterResolver,
                    function,
                    dataKind,
                    dataType,
                    { step },
                    { instance },
                    { old },
                    { new }
                ).resolve()

                continuationConverter.toContinuation(callable.call())
            }
        }
    }

    private companion object {
        fun canBeInvoked(function: KFunction<*>): Boolean {
            return function.visibility == KVisibility.PUBLIC && !function.isAbstract
        }
    }
}