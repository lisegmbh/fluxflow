package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.DataDefinition

open class ReflectedData<TInstance, TModel>(
    override val step: Step,
    override val definition: DataDefinition<TModel>,
    val instance: TInstance,
    private val propertyAccessor: PropertyGetter<TInstance, TModel>
) : Data<TModel> {
    override fun get(): TModel {
        return propertyAccessor.get(instance)
    }
}

