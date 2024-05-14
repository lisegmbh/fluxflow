package de.lise.fluxflow.validation.noop

import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.stereotyped.step.data.validation.ValidationBuilder
import de.lise.fluxflow.stereotyped.step.data.validation.ValidationBuilderResult
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class NoOpDataValidationBuilder : ValidationBuilder {
    override fun <TInstance : Any, TProp : Any?> buildValidations(
        dataKind: DataKind,
        instanceType: KClass<out TInstance>,
        prop: KProperty1<TInstance, TProp>
    ): ValidationBuilderResult<TInstance>? {
        return null
    }
}

