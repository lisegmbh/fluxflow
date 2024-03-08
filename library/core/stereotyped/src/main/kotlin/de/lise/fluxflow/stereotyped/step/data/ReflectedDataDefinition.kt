package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.*
import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition
import java.lang.reflect.Type

class ReflectedDataDefinition<TInstance, TModel>(
    override val kind: DataKind,
    override val type: Type,
    override val isCalculatedValue: Boolean,
    override val updateListeners: List<DataListenerDefinition<TModel>>,
    override val validation: DataValidationDefinition?,
    override val modificationPolicy: ModificationPolicy,
    private val instance: TInstance,
    private val propertyGetter: PropertyGetter<TInstance, TModel>,
    private val propertySetter: PropertySetter<TInstance, TModel>? = null,
) : DataDefinition<TModel> {
    override fun createData(step: Step): Data<TModel> {
        val readOnlyData = ReflectedData(
            step,
            this,
            instance,
            propertyGetter
        )

        return propertySetter?.let {
            ReflectedModifiableData(
                step,
                this,
                instance,
                propertyGetter,
                propertySetter
            )
        } ?: readOnlyData
    }
}