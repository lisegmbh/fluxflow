package de.lise.fluxflow.validation.jakarta

import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationConstraint

class JakartaDataValidationConstraint(
    override val name: String?,
    override val attributes: Map<String, Any>
): DataValidationConstraint