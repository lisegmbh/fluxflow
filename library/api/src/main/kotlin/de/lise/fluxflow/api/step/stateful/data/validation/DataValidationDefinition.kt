package de.lise.fluxflow.api.step.stateful.data.validation

import de.lise.fluxflow.api.step.stateful.data.Data

/**
 * A [DataValidationDefinition] describes how step data should to be validated.
 */
interface DataValidationDefinition {
    /**
     * A list of constraints to be applied on a step data value.
     */
    val constraints: List<DataValidationConstraint>

    /**
     * Creates a new [DataValidation] that can be used to perform the described validation.
     * @param data The data object that should be validated based on this definition.
     * @return A new data validation.
     * @throws IllegalArgumentException Is thrown if the provided data is incompatible with this definition.
     * This usually occurrs, if it is applied on a [Data] object this validation hasn't been defined for.
     */
    fun create(
        data: Data<*>
    ): DataValidation
}