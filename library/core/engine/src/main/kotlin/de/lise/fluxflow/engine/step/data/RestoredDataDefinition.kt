package de.lise.fluxflow.engine.step.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.DataDefinition
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.DataListenerDefinition
import de.lise.fluxflow.api.step.stateful.data.ModificationPolicy
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition
import de.lise.fluxflow.persistence.step.definition.DataDefinitionData
import java.lang.reflect.Type

class RestoredDataDefinition(
    override val kind: DataKind,
    override val metadata: Map<String, Any>,
    override val isCalculatedValue: Boolean,
    override val modificationPolicy: ModificationPolicy,
    private val value: Any?,
) : DataDefinition<Any?> {
    override val type: Type
        get() = value?.let { it::class.java } ?: Any::class.java
    override val updateListeners: List<DataListenerDefinition<Any?>>
        get() = emptyList()
    override val validation: DataValidationDefinition?
        get() = null
    override val isReadonly: Boolean
        get() = true

    constructor(
        dataDefinitionData: DataDefinitionData,
        value: Any?    
    ) : this(
        DataKind(dataDefinitionData.kind),
        dataDefinitionData.metadata,
        dataDefinitionData.isCalculatedValue,
        ModificationPolicy.PreventInactiveModification,
        value
    )

    override fun createData(step: Step): RestoredData {
        return RestoredData(
            step,
            this,
            value
        )
    }
}