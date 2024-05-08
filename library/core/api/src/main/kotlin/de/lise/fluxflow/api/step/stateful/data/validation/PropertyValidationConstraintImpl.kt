package de.lise.fluxflow.api.step.stateful.data.validation

/**
 * This is the default implementation of [PropertyValidationConstraint].
 */
data class PropertyValidationConstraintImpl(
    override val name: String?,
    override val attributes: Map<String, Any>,
    override val property: String,
    override val constraints: List<DataValidationConstraint>
) : PropertyValidationConstraint