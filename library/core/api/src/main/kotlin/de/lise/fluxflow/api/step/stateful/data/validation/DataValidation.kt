package de.lise.fluxflow.api.step.stateful.data.validation

import kotlin.reflect.KClass

/**
 * Represents an executable data validation.
 */
interface DataValidation {
    /**
     * Performs the actual validation.
     * @param groups The currently active validation groups.
     * @return A collection of issues that have been identified when running the evaluation.
     * Returns an empty collection, if no validation errors occur.
     */
    fun validate(
        groups: Set<KClass<*>>
    ): Collection<DataValidationIssue>
}