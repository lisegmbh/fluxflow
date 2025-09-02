package de.lise.fluxflow.api.step.stateful.data.validation

/**
 * This is a special type of [DataValidationConstraint],
 * indicating that a nested property should also be validated.
 */
interface PropertyValidationConstraint : DataValidationConstraint {
    /**
     * The name of the nested property.
     */
    val property: String

    /**
     * The constraints to be applied to the property.
     */
    val constraints: List<DataValidationConstraint>
}