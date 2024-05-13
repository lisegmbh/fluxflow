package de.lise.fluxflow.stereotyped.step.data.validation

import de.lise.fluxflow.api.step.stateful.data.DataKind
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * A [ValidationBuilder] is used to obtain validation information for a given data definition.
 */
interface ValidationBuilder {
    /**
     * Builds the validation information for the specified data kind represented by the property [prop] of type [instanceType].
     * @param dataKind The step data's kind.
     * @param instanceType The declaring type.
     * @param prop The property representing the step data.
     * @return The [ValidationBuilderResult] obtained from inspecting the given [prop].
     * If there are no validation constraints present, implementations must return `null`.
     */
    fun <TInstance: Any, TProp : Any?> buildValidations(
        dataKind: DataKind,
        instanceType: KClass<out TInstance>,
        prop: KProperty1<TInstance, TProp>,
    ): ValidationBuilderResult<TInstance>?
}