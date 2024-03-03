package de.lise.fluxflow.stereotyped.continuation

import de.lise.fluxflow.api.continuation.Continuation
import de.lise.fluxflow.api.continuation.StatusBehavior
import de.lise.fluxflow.stereotyped.step.action.ImplicitStatusBehavior
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf


class ContinuationBuilder {
    fun <T> createResultConverter(
        implicitStatusBehavior: ImplicitStatusBehavior,
        forceImplicitBehavior: Boolean,
        function: KFunction<T>
    ): ContinuationConverter<T> {
        val result = createPlainConverter(function)
        if (!result.isImplicit && !forceImplicitBehavior) {
            return result.converter
        }
        val plainConverter = result.converter
        
        return when(implicitStatusBehavior) {
            ImplicitStatusBehavior.Default -> plainConverter
            ImplicitStatusBehavior.Complete -> ContinuationConverter {
                plainConverter.toContinuation(it).withStatusBehavior(StatusBehavior.Complete)
            }
            ImplicitStatusBehavior.Cancel -> ContinuationConverter {
                plainConverter.toContinuation(it).withStatusBehavior(StatusBehavior.Cancel)
            }
            ImplicitStatusBehavior.Preserve -> ContinuationConverter {
                plainConverter.toContinuation(it).withStatusBehavior(StatusBehavior.Preserve)
            }
        }
    }

    private fun <T> createPlainConverter(
        function: KFunction<T>
    ): ConverterResult<T> {
        val returnType = function.returnType

        if (returnType == typeOf<Unit>()) {
            return ConverterResult(true) { _ -> Continuation.none() }
        }

        if (returnType.isSubtypeOf(typeOf<Continuation<*>>())) {
            return ConverterResult(false) { result -> result as Continuation<*> }
        }

        return ConverterResult(true) { result -> Continuation.step(result) }
    }
    
    data class ConverterResult<T>(
        val isImplicit: Boolean,
        val converter: ContinuationConverter<T>,
    )
}