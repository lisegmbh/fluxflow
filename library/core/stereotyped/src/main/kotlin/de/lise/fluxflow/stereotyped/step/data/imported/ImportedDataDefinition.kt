package de.lise.fluxflow.stereotyped.step.data.imported

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.*
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition
import java.lang.reflect.Type

data class ImportedDataDefinition<T>(
    private val nestedDefinition: DataDefinition<T>,
    private val additionalListeners: List<DataListenerDefinition<*>>,
) : DataDefinition<T> {
    override val kind: DataKind
        get() = nestedDefinition.kind

    override val type: Type
        get() = nestedDefinition.type

    override val metadata: Map<String, Any>
        get() = nestedDefinition.metadata

    override val isCalculatedValue: Boolean
        get() = nestedDefinition.isCalculatedValue

    override val updateListeners: List<DataListenerDefinition<T>>
        get() = nestedDefinition.updateListeners + (additionalListeners as List<DataListenerDefinition<T>>)

    override val validation: DataValidationDefinition?
        get() = nestedDefinition.validation

    override val modificationPolicy: ModificationPolicy
        get() = nestedDefinition.modificationPolicy

    override val isReadonly: Boolean
        get() = nestedDefinition.isReadonly

    override fun createData(step: Step): Data<T> {
        val originalData = nestedDefinition.createData(step)
        val decoratedData = DataWithOverwrittenDefinition(
            this,
            nestedDefinition.createData(step)
        )
        return when(originalData) {
            is ModifiableData -> DataWithDecoratedSetter(
                decoratedData,
            ) {
                originalData.set(it)
            }
            else -> decoratedData
        }
    }
}