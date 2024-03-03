package de.lise.fluxflow.stereotyped.step.data.validation

import de.lise.fluxflow.api.step.stateful.data.DataKind
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface ValidationBuilder {
    fun <TInstance: Any, TProp : Any> buildValidations(
        dataKind: DataKind,
        instanceType: KClass<out TInstance>,
        prop: KProperty1<TInstance, TProp>,
    ): ValidationBuilderResult<TInstance>?
}