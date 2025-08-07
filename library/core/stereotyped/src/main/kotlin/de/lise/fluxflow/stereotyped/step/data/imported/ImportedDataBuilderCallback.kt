package de.lise.fluxflow.stereotyped.step.data.imported

import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.stereotyped.step.data.DataBuilderCallback
import de.lise.fluxflow.stereotyped.step.data.DataListenerDefinitionBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class ImportedDataBuilderCallback<TObject : Any, TWrapper>(
    private val listenerDefinitionBuilder: DataListenerDefinitionBuilder,
    private val propertyToUnwrap: KProperty1<TObject, TWrapper>,
    private val nestedPropertyCallback: DataBuilderCallback<TWrapper>,
    private val importingType: KClass<*>,
) : DataBuilderCallback<TObject> {
    override fun invoke(instance: TObject): DataDefinition<*> {
        val importedInstance = propertyToUnwrap.get(instance)
        val result = nestedPropertyCallback.invoke(importedInstance)

        val definitionResults = listenerDefinitionBuilder.build<TObject, Any?>(
            result.kind,
            result.type,
            importingType
        ).map { it.invoke(instance) }

        val finalDefinition = ImportedDataDefinition(
            result,
            definitionResults
        )

        return finalDefinition
    }
}