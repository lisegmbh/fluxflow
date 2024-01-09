package de.lise.fluxflow.api.step.stateful.data.validation

import de.lise.fluxflow.api.step.stateful.action.Action

/**
 * This exception is thrown if an error occurred validating a step's data.
 * It might be caught by domain logic, in order to react to validation errors.
 */
class DataValidationException(
    /**
     * A description describing the current validation error.
     */
    message: String,
    /**
     * The action that failed validation
     */
    val action: Action,
    /**
     * A collection of all validation issues that have been found.
     */
    val issues: Collection<DataValidationIssue>
): Exception(message)