package de.lise.fluxflow.api.step.stateful.data

import de.lise.fluxflow.api.step.Step
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition
import java.lang.reflect.Type

interface DataDefinition<T> {
    val kind: DataKind
    val type: Type
    /**
     * Indicates if the data represented by this definition, will be dynamically calculated.
     * If this is set to true, the engine does not attempt to restore a previously persisted value.
     */
    val isCalculatedValue: Boolean

    /**
     * A list of listeners to be informed, if the step data defined by this instance changes.
     */
    val updateListeners: List<DataListenerDefinition<T>>

    /**
     * Holds information on how the data represented by this definition should be validated.
     * Is `null`, if validation is not required for the data.
     */
    val validation: DataValidationDefinition?

    /**
     * Returns `true`, if the step data is modifiable.
     */
    val isModifiable: Boolean
    fun createData(step: Step): Data<T>
}