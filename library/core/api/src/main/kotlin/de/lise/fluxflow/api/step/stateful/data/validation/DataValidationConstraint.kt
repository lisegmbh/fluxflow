package de.lise.fluxflow.api.step.stateful.data.validation

/**
 * Describes a constraint applied to a step data value.
 */
interface DataValidationConstraint {
    /**
     * A technical identifier describing this constraint.
     */
    val name: String?

    /**
     * Custom attributes relevant to the data validation constraint
     */
    val attributes: Map<String, Any>
}