package de.lise.fluxflow.api.step.stateful.data.validation

/**
 * This is the default implementation of [PropertyValidationConstraint].
 */
data class PropertyValidationConstraintImpl(
    override val name: String?,
    override val attributes: Map<String, Any>,
    override val property: String,
    override val constraints: List<DataValidationConstraint>
) : PropertyValidationConstraint {
    override fun toString(): String {
        // This method has been overwritten to avoid a stack overflow on cyclic child constraints,
        // when calling .toString of child constraints
        return "PropertyValidationConstraint(" +
                "name=${name}, " +
                "attributes=${attributes}, " +
                "property=${property}, " +
                "constraints=[..., size=${constraints.size}]" +
                ")"
    }
}