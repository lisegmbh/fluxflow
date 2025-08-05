package de.lise.fluxflow.stereotyped.step.data

import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import kotlin.reflect.KProperty1

class NestedDataBuilderCallback<TObject, TWrapper>(
    private val propertyToUnwrap: KProperty1<TObject, TWrapper>,
    private val nestedPropertyCallback: DataBuilderCallback<TWrapper>
) : DataBuilderCallback<TObject> {
    override fun invoke(instance: TObject): DataDefinition<*> {
        val importedInstance = propertyToUnwrap.get(instance)
        return nestedPropertyCallback.invoke(importedInstance)
    }
}