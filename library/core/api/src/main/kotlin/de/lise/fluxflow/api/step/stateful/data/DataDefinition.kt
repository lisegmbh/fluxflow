package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition
import java.lang.reflect.Type

interface DataDefinition<T> {
    val kind: DataKind
    val type: Type

    /**
     * A simple key-value map providing additional metadata about this data definition and data that are going to be produced by it.
     */
    val metadata: Map<String, Any>

    /**
     * Indicates if the data represented by this definition, will be dynamically calculated.
     * If this is set to true, the engine does not attempt to restore a previously persisted value.
     */
    val isCalculatedValue: Boolean

    /**
     * A list of listeners to be informed, whenever the step data defined by this instance changes.
     */
    val updateListeners: List<DataListenerDefinition<T>>

    /**
     * Holds information on how the data represented by this definition should be validated.
     * Is `null`, if validation is not required for the data.
     */
    val validation: DataValidationDefinition?

    /**
     * Controls if inactive data can be modified.
     */
    val modificationPolicy: ModificationPolicy

    /**
     * Indicates if this definition describes a read-only (see [Data])
     * or modifiable data element (see [ModifiableData]).
     */
    val isReadonly: Boolean

    fun createData(step: Step): Data<T>
}