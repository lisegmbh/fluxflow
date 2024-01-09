package de.lise.fluxflow.validation.jakarta

import de.lise.fluxflow.api.step.stateful.data.Data
import de.lise.fluxflow.api.step.stateful.data.DataKind
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidation
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationConstraint
import de.lise.fluxflow.api.step.stateful.data.validation.DataValidationDefinition
import de.lise.fluxflow.stereotyped.step.data.ReflectedData
import jakarta.validation.Validator

class JakartaValidationDefinition(
    override val constraints: List<DataValidationConstraint>,
    private val dataKind: DataKind,
    private val validator: Validator,
    private val propertyName: String
) : DataValidationDefinition {
    override fun create(
        data: Data<*>
    ): DataValidation {
        val instance = (data as? ReflectedData<*, *>)?.instance ?: throw IllegalArgumentException(
            "The supplied data object can not be validation using this validation definition"
        )

        return JakartaValidation(
            dataKind,
            validator,
            instance,
            propertyName
        )
    }
}