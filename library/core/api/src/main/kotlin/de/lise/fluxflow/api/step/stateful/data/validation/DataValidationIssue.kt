package de.lise.fluxflow.api.step.stateful.data.validation

import de.lise.fluxflow.api.step.stateful.data.DataKind

/**
 * Describes an issue/error that was found when validating a step's data.
 */
data class DataValidationIssue(
    /**
     * The kind of the affected step data.
     */
    val dataKind: DataKind,
    /**
     * Holds the constraint that has been violated.
     * Might be `null`, if the issue could not be associated with a distinct constraint.
     */
    val constraint: DataValidationConstraint,
    /**
     * The path at which the validation error occurred.
     */
    val path: String,
    /**
     * A human-readable representation of this validation error.
     */
    val message: String,
    /**
     * The offending value.
     * Might be null if the violation is not related to a (single) value or the value has been null.
     */
    val invalidValue: Any?
)