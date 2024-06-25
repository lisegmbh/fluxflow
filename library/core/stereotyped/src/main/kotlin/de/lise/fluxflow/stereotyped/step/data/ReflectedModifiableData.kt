package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.step.stateful.data.ModifiableData

class ReflectedModifiableData<TInstance, TModel>(
    step: Step,
    definition: DataDefinition<TModel>,
    instance: TInstance,
    getter: PropertyGetter<TInstance, TModel>,
    private val propertyAccessor: PropertySetter<TInstance, TModel>
) : ReflectedData<TInstance, TModel>(
    step,
    definition,
    instance,
    getter
), ModifiableData<TModel> {

    override fun set(value: TModel) {
        try {
            propertyAccessor.set(instance, value)
        } catch (ex: Exception) {
            throw StepDataAssignmentException(
                value,
                definition,
                step,
                ex
            )
        }
    }

}